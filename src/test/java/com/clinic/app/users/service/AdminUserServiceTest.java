package com.clinic.app.users.service;

import static org.assertj.core.api.Assertions.*;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;

import com.clinic.app.users.domain.AppUser;
import com.clinic.app.users.domain.Role;
import com.clinic.app.users.repo.AppUserRepository;

import jakarta.annotation.Resource;

@DataJpaTest(properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import(AdminUserServiceTest.TestConfig.class)
class AdminUserServiceTest {

  @Resource
  private AppUserRepository userRepo;

  @Resource
  private AdminUserService adminUserService;

  @TestConfiguration
  static class TestConfig {

    @Bean
    Clock clock() {
      return Clock.fixed(Instant.parse("2026-03-04T10:00:00Z"), ZoneOffset.UTC);
    }

    @Bean
    AdminUserService adminUserService(AppUserRepository repo) {
      return new AdminUserService(repo);
    }
  }

  private AppUser seedUser(String uid, String email, Role role, boolean enabled) {
    OffsetDateTime now = OffsetDateTime.ofInstant(Instant.parse("2026-03-04T10:00:00Z"), ZoneOffset.UTC);
    return userRepo.save(AppUser.builder()
        .firebaseUid(uid)
        .email(email)
        .role(role)
        .enabled(enabled)
        .createdAt(now)
        .build());
  }

  @Test
  void setEnabled_cannotDisableLastEnabledAdmin() {
    AppUser admin = seedUser("uid-admin-1", "admin1@x.com", Role.ADMIN, true);

    assertThatThrownBy(() -> adminUserService.setEnabled(admin.getId(), false))
        // si usas tus excepciones shared, cambia a ConflictException
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  void updateRole_cannotRemoveRoleFromLastEnabledAdmin() {
    AppUser admin = seedUser("uid-admin-1", "admin1@x.com", Role.ADMIN, true);

    assertThatThrownBy(() -> adminUserService.updateRole(admin.getId(), Role.DENTIST))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  void setEnabled_allowsDisableIfThereIsAnotherEnabledAdmin() {
    AppUser admin1 = seedUser("uid-admin-1", "admin1@x.com", Role.ADMIN, true);
    seedUser("uid-admin-2", "admin2@x.com", Role.ADMIN, true);

    AppUser updated = adminUserService.setEnabled(admin1.getId(), false);

    assertThat(updated.isEnabled()).isFalse();
  }

  @Test
  void updateRole_allowsRoleChangeIfThereIsAnotherEnabledAdmin() {
    AppUser admin1 = seedUser("uid-admin-1", "admin1@x.com", Role.ADMIN, true);
    seedUser("uid-admin-2", "admin2@x.com", Role.ADMIN, true);

    AppUser updated = adminUserService.updateRole(admin1.getId(), Role.DENTIST);

    assertThat(updated.getRole()).isEqualTo(Role.DENTIST);
  }

  @Test
  void setEnabled_throwsNotFoundForUnknownId() {
    assertThatThrownBy(() -> adminUserService.setEnabled(9999L, false))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  void updateRole_throwsNotFoundForUnknownId() {
    assertThatThrownBy(() -> adminUserService.updateRole(9999L, Role.DENTIST))
        .isInstanceOf(RuntimeException.class);
  }
}