package com.clinic.app.users.service;

import java.time.Clock;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.clinic.app.users.domain.UserProfile;
import com.clinic.app.users.repo.UserProfileRepository;

@Service
public class UserProfileService {

  private final UserProfileRepository profileRepo;
  private final Clock clock;

  public UserProfileService(UserProfileRepository profileRepo, Clock clock) {
    this.profileRepo = profileRepo;
    this.clock = clock;
  }

  public boolean profileExists(Long userId) {
    return profileRepo.existsById(userId);
  }

  public UserProfile getProfileOrNull(Long userId) {
    return profileRepo.findById(userId).orElse(null);
  }

  @Transactional
  public UserProfile upsertProfile(Long userId, String fullName, String phone) {
    OffsetDateTime now = OffsetDateTime.now(clock);

    String fn = fullName == null ? "" : fullName.trim();
    String ph = phone == null ? "" : phone.trim();

    return profileRepo.findById(userId)
        .map(p -> {
          p.update(fn, ph, now);
          return p;
        })
        .orElseGet(() -> profileRepo.save(new UserProfile(userId, fn, ph, now)));
  }
}