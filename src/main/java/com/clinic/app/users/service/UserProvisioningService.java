package com.clinic.app.users.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.clinic.app.users.domain.AppUser;
import com.clinic.app.users.domain.Role;
import com.clinic.app.users.repo.AppUserRepository;

/**
 * Single entry-point for creating/updating local users based on Firebase authentication.
 * Keeps provisioning logic out of security filters and out of invitations.
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
   * Policy:
   * - If user exists by firebaseUid: ensure email is up-to-date and enabled=true
   * - If no user by uid:
   *    - If email already used by a different user: conflict
   *    - Else create new PATIENT enabled=true
   *
   * NOTE: email must be present (because app_user.email is NOT NULL and UNIQUE).
   */
  @Transactional
  public AppUser provisionOnLogin(String firebaseUid, String emailRaw) {
    Objects.requireNonNull(firebaseUid, "firebaseUid");

    String email = normalizeEmail(emailRaw);
    if (email == null || email.isBlank()) {
      throw new BadRequest("Missing email for provisioning");
    }

    OffsetDateTime now = OffsetDateTime.now(clock);

    // 1) Existing by uid
    var byUid = userRepo.findByFirebaseUid(firebaseUid);
    if (byUid.isPresent()) {
      AppUser u = byUid.get();

      // If email differs, update but guard against collision
      if (!u.getEmail().equalsIgnoreCase(email)) {
        userRepo.findByEmailIgnoreCase(email).ifPresent(other -> {
          if (!other.getId().equals(u.getId())) {
            throw new Conflict("Email already used by another user");
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
      throw new Conflict("Email already registered with a different account");
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
   * Ensure there is a local user with this firebaseUid/email and assign staff role.
   */
  @Transactional
  public AppUser createOrUpdateStaffFromInvitation(String firebaseUid, String emailRaw, Role staffRole) {
    Objects.requireNonNull(firebaseUid, "firebaseUid");
    String email = normalizeEmail(emailRaw);
    if (email == null || email.isBlank()) {
      throw new BadRequest("Missing email for staff promotion");
    }
    Objects.requireNonNull(staffRole, "staffRole");

    if (staffRole == Role.PATIENT) {
      throw new BadRequest("Staff invitation cannot assign PATIENT role");
    }

    OffsetDateTime now = OffsetDateTime.now(clock);

    // Prefer lookup by uid first
    var byUid = userRepo.findByFirebaseUid(firebaseUid);
    if (byUid.isPresent()) {
      AppUser u = byUid.get();

      if (!u.getEmail().equalsIgnoreCase(email)) {
        userRepo.findByEmailIgnoreCase(email).ifPresent(other -> {
          if (!other.getId().equals(u.getId())) {
            throw new Conflict("Email already used by another user");
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
      throw new Conflict("Email already registered with a different account");
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

  // Replace later with your shared exceptions (ConflictException, BadRequestException, etc.)
  public static class BadRequest extends RuntimeException { public BadRequest(String m) { super(m); } }
  public static class Conflict extends RuntimeException { public Conflict(String m) { super(m); } }
}