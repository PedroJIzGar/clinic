package com.clinic.app.users.service;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.clinic.app.users.UserProfile;
import com.clinic.app.users.repository.UserProfileRepository;

@Service
public class UserProfileService {

  private final UserProfileRepository profileRepo;

  public UserProfileService(UserProfileRepository profileRepo) {
    this.profileRepo = profileRepo;
  }

  public boolean profileExists(Long userId) {
    return profileRepo.existsById(userId);
  }

  public UserProfile getProfileOrNull(Long userId) {
    return profileRepo.findById(userId).orElse(null);
  }

  @Transactional
  public UserProfile upsertProfile(Long userId, String fullName, String phone) {
    OffsetDateTime now = OffsetDateTime.now();

    return profileRepo.findById(userId)
        .map(p -> {
          p.update(fullName.trim(), phone.trim(), now);
          return p;
        })
        .orElseGet(() -> profileRepo.save(new UserProfile(userId, fullName.trim(), phone.trim(), now)));
  }
}