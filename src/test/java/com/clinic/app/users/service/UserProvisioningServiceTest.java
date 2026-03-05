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

import com.clinic.app.shared.exception.ConflictException;
import com.clinic.app.users.domain.AppUser;
import com.clinic.app.users.domain.Role;
import com.clinic.app.users.repo.AppUserRepository;

import jakarta.annotation.Resource;

@DataJpaTest(properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import(UserProvisioningServiceTest.TestConfig.class)
class UserProvisioningServiceTest {

  @Resource
  private AppUserRepository userRepo;

  @Resource
  private UserProvisioningService provisioning;

  @TestConfiguration
  static class TestConfig {
    @Bean
    Clock clock() {
      return Clock.fixed(Instant.parse("2026-03-04T10:00:00Z"), ZoneOffset.UTC);
    }

    @Bean
    UserProvisioningService userProvisioningService(AppUserRepository repo, Clock clock) {
      return new UserProvisioningService(repo, clock);
    }
  }

  @Test
  void provisionOnLogin_createsNewPatient() {
    AppUser u = provisioning.provisionOnLogin("uid-1", "Test@Email.com");

    assertThat(u.getId()).isNotNull();
    assertThat(u.getFirebaseUid()).isEqualTo("uid-1");
    assertThat(u.getEmail()).isEqualTo("test@email.com");
    assertThat(u.getRole()).isEqualTo(Role.PATIENT);
    assertThat(u.isEnabled()).isTrue();
    assertThat(u.getCreatedAt()).isEqualTo(OffsetDateTime.ofInstant(
        Instant.parse("2026-03-04T10:00:00Z"), ZoneOffset.UTC
    ));

    // persisted
    assertThat(userRepo.findByFirebaseUid("uid-1")).isPresent();
  }

  @Test
  void provisionOnLogin_updatesEmailWhenUidExists() {
    provisioning.provisionOnLogin("uid-1", "old@email.com");

    AppUser updated = provisioning.provisionOnLogin("uid-1", "NEW@Email.com");

    assertThat(updated.getEmail()).isEqualTo("new@email.com");
    assertThat(updated.getRole()).isEqualTo(Role.PATIENT);
  }

  @Test
  void provisionOnLogin_conflictWhenEmailBelongsToDifferentUid() {
    provisioning.provisionOnLogin("uid-1", "same@email.com");

    assertThatThrownBy(() -> provisioning.provisionOnLogin("uid-2", "same@email.com"))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void createOrUpdateStaffFromInvitation_createsStaffUser() {
    AppUser staff = provisioning.createOrUpdateStaffFromInvitation("uid-staff", "staff@email.com", Role.DENTIST);

    assertThat(staff.getRole()).isEqualTo(Role.DENTIST);
    assertThat(staff.isEnabled()).isTrue();
    assertThat(staff.getEmail()).isEqualTo("staff@email.com");
  }

  @Test
  void createOrUpdateStaffFromInvitation_updatesExistingUserRole() {
    provisioning.provisionOnLogin("uid-1", "user@email.com"); // creates PATIENT

    AppUser staff = provisioning.createOrUpdateStaffFromInvitation("uid-1", "user@email.com", Role.RECEPTIONIST);

    assertThat(staff.getRole()).isEqualTo(Role.RECEPTIONIST);
    assertThat(staff.isEnabled()).isTrue();
  }

  @Test
  void createOrUpdateStaffFromInvitation_rejectsPatientRole() {
    assertThatThrownBy(() -> provisioning.createOrUpdateStaffFromInvitation("uid-1", "x@y.com", Role.PATIENT))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void provisionOnLogin_rejectsMissingEmail() {
    assertThatThrownBy(() -> provisioning.provisionOnLogin("uid-1", "  "))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
