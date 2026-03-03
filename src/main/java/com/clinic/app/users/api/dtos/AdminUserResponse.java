package com.clinic.app.users.api.dtos;

import java.time.OffsetDateTime;

import com.clinic.app.users.domain.AppUser;
import com.clinic.app.users.domain.Role;

public record AdminUserResponse(
    Long id,
    String firebaseUid,
    String email,
    Role role,
    boolean enabled,
    OffsetDateTime createdAt
) {
  public static AdminUserResponse from(AppUser u) {
    return new AdminUserResponse(
        u.getId(),
        u.getFirebaseUid(),
        u.getEmail(),
        u.getRole(),
        u.isEnabled(),
        u.getCreatedAt()
    );
  }
}