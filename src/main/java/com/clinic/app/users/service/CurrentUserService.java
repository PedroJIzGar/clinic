package com.clinic.app.users.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.clinic.app.shared.exception.NotFoundException;
import com.clinic.app.shared.security.FirebasePrincipal;
import com.clinic.app.users.domain.AppUser;
import com.clinic.app.users.domain.Role;
import com.clinic.app.users.repo.AppUserRepository;

@Service
public class CurrentUserService {

  private final AppUserRepository appUserRepository;

  public CurrentUserService(AppUserRepository appUserRepository) {
    this.appUserRepository = appUserRepository;
  }

  public FirebasePrincipal requirePrincipal() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getPrincipal() == null) {
      throw new InsufficientAuthenticationException("Missing authentication");
    }

    if (!(auth.getPrincipal() instanceof FirebasePrincipal principal)) {
      throw new InsufficientAuthenticationException("Unexpected principal type");
    }

    if (principal.uid() == null || principal.uid().isBlank()) {
      throw new InsufficientAuthenticationException("Missing uid");
    }

    return principal;
  }

  /**
   * Returns the DB user for the currently authenticated Firebase principal.
   * If authenticated but not provisioned in DB -> NotFoundException (404).
   */
  public AppUser requireCurrentUser() {
    FirebasePrincipal principal = requirePrincipal();

    String uid = principal.uid();
    String email = principal.email() == null ? "" : principal.email().trim().toLowerCase();

    return appUserRepository.findByFirebaseUid(uid)
        .or(() -> email.isBlank() ? java.util.Optional.empty() : appUserRepository.findByEmailIgnoreCase(email))
        .orElseThrow(() -> new NotFoundException("Authenticated user not provisioned in DB"));
  }

  public Long requireCurrentUserId() {
    return requireCurrentUser().getId();
  }

  /**
   * For admin services: ensures the current DB user exists and is ADMIN.
   * If not ADMIN -> AccessDeniedException (403).
   */
  public AppUser requireAdminUser() {
    AppUser u = requireCurrentUser();
    if (u.getRole() != Role.ADMIN) {
      throw new AccessDeniedException("Admin role required");
    }
    return u;
  }

  public Long requireAdminUserId() {
    return requireAdminUser().getId();
  }
}