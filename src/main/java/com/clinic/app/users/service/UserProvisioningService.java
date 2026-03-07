package com.clinic.app.users.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.clinic.app.shared.exception.ConflictException;
import com.clinic.app.users.domain.AppUser;
import com.clinic.app.users.domain.Role;
import com.clinic.app.users.repo.AppUserRepository;

/**
 * User provisioning rules (enterprise-style):
 * - Auth filter MUST NOT create users. It only authenticates and (optionally)
 * loads roles if user exists.
 * - Staff users are provisioned ONLY via invitations.accept().
 * - Patient users are provisioned ONLY via an explicit registration endpoint
 * (see registerPatientIfMissing()).
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
   * Auth filter helper: lookup only, never creates.
   * - First by firebaseUid
   * - Then by email
   */
  @Transactional(readOnly = true)
  public Optional<AppUser> findExistingByUidOrEmail(String firebaseUid, String emailRaw) {
    if (firebaseUid == null || firebaseUid.isBlank()) {
      return Optional.empty();
    }

    Optional<AppUser> byUid = userRepo.findByFirebaseUid(firebaseUid);
    if (byUid.isPresent()) {
      return byUid;
    }

    if (emailRaw == null || emailRaw.isBlank()) {
      return Optional.empty();
    }

    String email = normalizeEmail(emailRaw);
    return userRepo.findByEmailIgnoreCase(email);
  }

  /**
   * Patient provisioning: called ONLY from an explicit endpoint (not from auth
   * filter).
   * Policy:
   * - If exists by uid: update email if changed, enable user. DO NOT change role.
   * - If not exists by uid but exists by email: link uid, enable user. DO NOT
   * change role.
   * - If brand new: create PATIENT enabled=true.
   */
  @Transactional
  public AppUser registerPatientIfMissing(String firebaseUid, String emailRaw) {

    if (firebaseUid == null || firebaseUid.isBlank()) {
      throw new IllegalArgumentException("firebaseUid is required");
    }
    if (emailRaw == null || emailRaw.isBlank()) {
      throw new IllegalArgumentException("email is required");
    }

    String email = normalizeEmail(emailRaw);
    OffsetDateTime now = OffsetDateTime.now(clock);

    // 1) by uid
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

      // ✅ do NOT touch role here
      u.setEnabled(true);
      return userRepo.save(u);
    }

    // 2) by email (link account)
    var byEmail = userRepo.findByEmailIgnoreCase(email);
    if (byEmail.isPresent()) {
      AppUser u = byEmail.get();

      if (u.getFirebaseUid() == null || u.getFirebaseUid().isBlank()) {
        u.setFirebaseUid(firebaseUid);
      } else if (!u.getFirebaseUid().equals(firebaseUid)) {
        throw new ConflictException("Email already registered with a different account");
      }

      // ✅ do NOT touch role here
      u.setEnabled(true);
      return userRepo.save(u);
    }

    // 3) brand new user -> create patient
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
   * Staff provisioning: used by invitations.accept().
   * Ensures there is a local user with this uid/email and assigns staff role.
   */
  @Transactional
  public AppUser createOrUpdateStaffFromInvitation(String firebaseUid, String emailRaw, Role staffRole) {

    if (firebaseUid == null || firebaseUid.isBlank()) {
      throw new IllegalArgumentException("firebaseUid is required for staff provisioning");
    }
    if (emailRaw == null || emailRaw.isBlank()) {
      throw new IllegalArgumentException("Email is required for staff provisioning");
    }
    if (staffRole == null) {
      throw new IllegalArgumentException("Staff role is required for staff provisioning");
    }
    if (staffRole == Role.PATIENT) {
      throw new IllegalArgumentException("Staff role cannot be PATIENT");
    }

    String email = normalizeEmail(emailRaw);
    OffsetDateTime now = OffsetDateTime.now(clock);

    // 1) by uid
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

    // 2) No user by uid: ensure email not used by another account
    userRepo.findByEmailIgnoreCase(email).ifPresent(other -> {
      throw new ConflictException("Email already registered with a different account");
    });

    // 3) Create staff
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
    return email == null ? "" : email.trim().toLowerCase();
  }
}