package com.clinic.app.users.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.clinic.app.users.domain.AppUser;
import com.clinic.app.users.repo.repository.AppUserRepository;

@Service
public class CurrentUserService {

  private final AppUserRepository appUserRepository;

  public CurrentUserService(AppUserRepository appUserRepository) {
    this.appUserRepository = appUserRepository;
  }

  public AppUser requireCurrentUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
      throw new IllegalStateException("Missing authentication");
    }

    String firebaseUid = auth.getName();
    return appUserRepository.findByFirebaseUid(firebaseUid)
        .orElseThrow(() -> new IllegalStateException("Authenticated user not found in DB"));
  }
}
