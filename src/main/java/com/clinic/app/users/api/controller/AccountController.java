package com.clinic.app.users.api.controller;

import com.clinic.app.users.AppUser;
import com.clinic.app.users.UserProfile;
import com.clinic.app.users.api.dtos.MyProfileResponse;
import com.clinic.app.users.api.dtos.MyUserResponse;
import com.clinic.app.users.api.dtos.UpdateMyProfileRequest;
import com.clinic.app.users.service.CurrentUserService;
import com.clinic.app.users.service.UserProfileService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account")
public class AccountController {

  private final CurrentUserService currentUserService;
  private final UserProfileService profileService;

  public AccountController(CurrentUserService currentUserService, UserProfileService profileService) {
    this.currentUserService = currentUserService;
    this.profileService = profileService;
  }

  @GetMapping
  public MyUserResponse me() {
    AppUser user = currentUserService.requireCurrentUser();
    boolean completed = profileService.profileExists(user.getId());

    return new MyUserResponse(
        user.getFirebaseUid(),
        user.getEmail(),
        user.getRole().name(),
        completed
    );
  }

  @GetMapping("/profile")
  public ResponseEntity<MyProfileResponse> myProfile() {
    AppUser user = currentUserService.requireCurrentUser();
    UserProfile profile = profileService.getProfileOrNull(user.getId());
    if (profile == null) return ResponseEntity.notFound().build();

    return ResponseEntity.ok(new MyProfileResponse(
        profile.getFullName(),
        profile.getPhone(),
        profile.getUpdatedAt().toString()
    ));
  }

  @PutMapping("/profile")
  public MyProfileResponse upsertMyProfile(@Valid @RequestBody UpdateMyProfileRequest req) {
    AppUser user = currentUserService.requireCurrentUser();
    UserProfile p = profileService.upsertProfile(user.getId(), req.fullName(), req.phone());

    return new MyProfileResponse(
        p.getFullName(),
        p.getPhone(),
        p.getUpdatedAt().toString()
    );
  }
}
