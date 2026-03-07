package com.clinic.app.users.api.controller;

import com.clinic.app.shared.security.FirebasePrincipal;
import com.clinic.app.users.api.dto.MyProfileResponse;
import com.clinic.app.users.api.dto.MyUserResponse;
import com.clinic.app.users.api.dto.UpdateMyProfileRequest;
import com.clinic.app.users.domain.AppUser;
import com.clinic.app.users.domain.UserProfile;
import com.clinic.app.users.service.CurrentUserService;
import com.clinic.app.users.service.UserProfileService;
import com.clinic.app.users.service.UserProvisioningService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account")
public class AccountController {

  private final CurrentUserService currentUserService;
  private final UserProfileService profileService;
  private final UserProvisioningService userProvisioningService;

  public AccountController(
      CurrentUserService currentUserService,
      UserProfileService profileService,
      UserProvisioningService userProvisioningService) {
    this.currentUserService = currentUserService;
    this.profileService = profileService;
    this.userProvisioningService = userProvisioningService;
  }

  /**
   * Explicit patient registration.
   * - Requires Firebase auth (uid+email)
   * - Creates a PATIENT AppUser only if missing
   * - Does NOT affect staff users (if already staff, it won't change role)
   */
  @PostMapping("/register-patient")
  public MyUserResponse registerPatient() {
    FirebasePrincipal principal = currentUserService.requirePrincipal();

    AppUser user = userProvisioningService.registerPatientIfMissing(
        principal.uid(),
        principal.email());

    boolean completed = profileService.profileExists(user.getId());
    return new MyUserResponse(
        user.getFirebaseUid(),
        user.getEmail(),
        user.getRole().name(),
        completed);
  }

  @GetMapping
  public MyUserResponse me() {
    // PRO: this should throw if user not provisioned in DB
    AppUser user = currentUserService.requireCurrentUser();
    boolean completed = profileService.profileExists(user.getId());

    return new MyUserResponse(
        user.getFirebaseUid(),
        user.getEmail(),
        user.getRole().name(),
        completed);
  }

  @GetMapping("/profile")
  public ResponseEntity<MyProfileResponse> myProfile() {
    AppUser user = currentUserService.requireCurrentUser();
    UserProfile profile = profileService.getProfileOrNull(user.getId());
    if (profile == null)
      return ResponseEntity.notFound().build();

    return ResponseEntity.ok(new MyProfileResponse(
        profile.getFullName(),
        profile.getPhone(),
        profile.getUpdatedAt()));
  }

  @PutMapping("/profile")
  public MyProfileResponse upsertMyProfile(@Valid @RequestBody UpdateMyProfileRequest req) {
    AppUser user = currentUserService.requireCurrentUser();
    UserProfile p = profileService.upsertProfile(user.getId(), req.fullName(), req.phone());

    return new MyProfileResponse(
        p.getFullName(),
        p.getPhone(),
        p.getUpdatedAt());
  }
}