package com.clinic.app.users.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.clinic.app.shared.security.FirebasePrincipal;
import com.clinic.app.users.domain.AppUser;
import com.clinic.app.users.repo.AppUserRepository;

@Service
public class CurrentUserService {

  private final AppUserRepository appUserRepository;

  public CurrentUserService(AppUserRepository appUserRepository) {
    this.appUserRepository = appUserRepository;
  }

  public AppUser requireCurrentUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getPrincipal() == null) {
      throw new IllegalStateException("Missing authentication");
    }

    if (!(auth.getPrincipal() instanceof FirebasePrincipal principal)) {
      throw new IllegalStateException("Unexpected principal type: " + auth.getPrincipal().getClass().getName());
    }

    String firebaseUid = principal.uid();

    return appUserRepository.findByFirebaseUid(firebaseUid)
        .orElseThrow(() -> new IllegalStateException("Authenticated user not found in DB"));
  }
}