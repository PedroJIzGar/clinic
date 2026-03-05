package com.clinic.app.users.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.clinic.app.shared.exception.ConflictException;
import com.clinic.app.users.domain.AppUser;
import com.clinic.app.users.domain.Role;
import com.clinic.app.users.repo.AppUserRepository;

/**
 * Single entry-point for creating/updating local users based on Firebase authentication.
 */
@Service
public class UserProvisioningService {

  private final AppUserRepository userRepo;
  private final Clock clock;

  public UserProvisioningService(AppUserRepository userRepo, Clock clock) {
    this.userRepo = userRepo;
    this.clock = clock;
  }

  /**
   * Called when a Firebase-authenticated user hits the API.
   * Policy:
   * - If exists by uid: update email if changed, enable user.
   * - If not exists by uid: ensure email not used, create PATIENT enabled=true.
   */
  @Transactional
  public AppUser provisionOnLogin(String firebaseUid, String emailRaw) {
    Objects.requireNonNull(firebaseUid, "firebaseUid");

    String email = normalizeEmail(emailRaw);
    if (email == null || email.isBlank()) {
      throw new IllegalArgumentException("Missing email for provisioning");
    }

    OffsetDateTime now = OffsetDateTime.now(clock);

    // 1) Existing by uid
    var byUid = userRepo.findByFirebaseUid(firebaseUid);
    if (byUid.isPresent()) {
      AppUser u = byUid.get();

      if (!u.getEmail().equalsIgnoreCase(email)) {
        userRepo.findByEmailIgnoreCase(email).ifPresent(other -> {
          if (!other.getId().equals(u.getId())) {
            throw new ConflictException("Email already used by another user");
          }
        });
        u.setEmail(email);
      }

      if (!u.isEnabled()) {
        u.setEnabled(true);
      }

      return userRepo.save(u);
    }

    // 2) No user by uid: ensure email not used
    userRepo.findByEmailIgnoreCase(email).ifPresent(other -> {
      throw new ConflictException("Email already registered with a different account");
    });

    // 3) Create new PATIENT
    AppUser created = AppUser.builder()
        .firebaseUid(firebaseUid)
        .email(email)
        .role(Role.PATIENT)
        .enabled(true)
        .createdAt(now)
        .build();

    return userRepo.save(created);
  }

  /**
   * Used by invitations.accept():
   * Ensure there is a local user with this uid/email and assign staff role.
   */
  @Transactional
  public AppUser createOrUpdateStaffFromInvitation(String firebaseUid, String emailRaw, Role staffRole) {
    Objects.requireNonNull(firebaseUid, "firebaseUid");
    Objects.requireNonNull(staffRole, "staffRole");

    String email = normalizeEmail(emailRaw);
    if (email == null || email.isBlank()) {
      throw new IllegalArgumentException("Missing email for staff promotion");
    }

    if (staffRole == Role.PATIENT) {
      throw new IllegalArgumentException("Staff invitation cannot assign PATIENT role");
    }

    OffsetDateTime now = OffsetDateTime.now(clock);

    var byUid = userRepo.findByFirebaseUid(firebaseUid);
    if (byUid.isPresent()) {
      AppUser u = byUid.get();

      if (!u.getEmail().equalsIgnoreCase(email)) {
        userRepo.findByEmailIgnoreCase(email).ifPresent(other -> {
          if (!other.getId().equals(u.getId())) {
            throw new ConflictException("Email already used by another user");
          }
        });
        u.setEmail(email);
      }

      u.setRole(staffRole);
      u.setEnabled(true);
      return userRepo.save(u);
    }

    // No user by uid: ensure email not used
    userRepo.findByEmailIgnoreCase(email).ifPresent(other -> {
      throw new ConflictException("Email already registered with a different account");
    });

    AppUser created = AppUser.builder()
        .firebaseUid(firebaseUid)
        .email(email)
        .role(staffRole)
        .enabled(true)
        .createdAt(now)
        .build();

    return userRepo.save(created);
  }

  private String normalizeEmail(String email) {
    return email == null ? null : email.trim().toLowerCase();
  }
}