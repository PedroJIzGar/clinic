package com.clinic.app.users.service;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.clinic.app.users.domain.Role;
import com.clinic.app.users.repo.AppUserRepository;

/**
 * Resolves a user's role from the local database.
 *
 * IMPORTANT:
 * - No side effects here (no user creation, no invitation acceptance).
 * - Provisioning is handled by UserProvisioningService.
 * - Invitation acceptance is handled by InvitationService.
 */
@Service
public class DbUserRoleResolver implements UserRoleResolver {

  private final AppUserRepository userRepo;

  public DbUserRoleResolver(AppUserRepository userRepo) {
    this.userRepo = userRepo;
  }

  @Override
  @Transactional(readOnly = true)
  public Role resolveRole(String firebaseUid, String email) {
    Objects.requireNonNull(firebaseUid, "firebaseUid");

    return userRepo.findByFirebaseUid(firebaseUid)
        .map(u -> u.getRole())
        // Fallback: if user is not found, treat as PATIENT (safe default)
        .orElse(Role.PATIENT);
  }
}