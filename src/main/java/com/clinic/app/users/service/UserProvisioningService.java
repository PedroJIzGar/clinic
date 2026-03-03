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
 * Keeps user creation logic out of security filters and out of invitations.
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
   * Policy (recommended):
   * - If user exists by firebaseUid: ensure email is up to date (if changed) and return.
   * - If not exists by firebaseUid:
   *    - If email already belongs to another user: conflict (protects against account mismatch)
   *    - Else create new user as PATIENT enabled=true
   */
  @Transactional
  public AppUser provisionOnLogin(String firebaseUid, String emailRaw) {
    Objects.requireNonNull(firebaseUid, "firebaseUid");
    Objects.requireNonNull(emailRaw, "email");

    String email = normalizeEmail(emailRaw);
    OffsetDateTime now = OffsetDateTime.now(clock);

    // 1) Existing by uid
    var byUid = userRepo.findByFirebaseUid(firebaseUid);
    if (byUid.isPresent()) {
      AppUser u = byUid.get();

      // Email might change in Firebase providers; update if needed
      if (u.getEmail() == null || !u.getEmail().equalsIgnoreCase(email)) {
        // But guard against email collisions
        userRepo.findByEmailIgnoreCase(email).ifPresent(other -> {
          if (!other.getId().equals(u.getId())) {
            throw new Conflict("Email already used by another user");
          }
        });
        u.setEmail(email);
      }

      // If your policy is to always enable on login:
      if (!u.isEnabled()) {
        u.setEnabled(true);
      }

      return userRepo.save(u);
    }

    // 2) No user by uid: ensure email not already used
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
    Objects.requireNonNull(emailRaw, "email");
    Objects.requireNonNull(staffRole, "staffRole");

    if (staffRole == Role.PATIENT) {
      throw new BadRequest("Staff invitation cannot assign PATIENT role");
    }

    String email = normalizeEmail(emailRaw);
    OffsetDateTime now = OffsetDateTime.now(clock);

    // Prefer lookup by uid first
    var byUid = userRepo.findByFirebaseUid(firebaseUid);
    if (byUid.isPresent()) {
      AppUser u = byUid.get();

      // If email differs, update but check collisions
      if (u.getEmail() == null || !u.getEmail().equalsIgnoreCase(email)) {
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
    return email.trim().toLowerCase();
  }

  // --- Minimal exceptions (swap with shared ones later) ---
  public static class BadRequest extends RuntimeException { public BadRequest(String m) { super(m); } }
  public static class Conflict extends RuntimeException { public Conflict(String m) { super(m); } }
}