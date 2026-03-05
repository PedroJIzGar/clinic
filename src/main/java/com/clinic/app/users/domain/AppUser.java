package com.clinic.app.users.domain;

import java.time.OffsetDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "app_user")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AppUser {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "firebase_uid", nullable = false, unique = true, length = 128)
  private String firebaseUid;

  @Column(nullable = false, unique = true)
  private String email;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private Role role;

  @Column(nullable = false)
  private boolean enabled;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  /**
   * Preferred factory: caller supplies 'now' (service uses Clock).
   */
  public static AppUser createNewPatient(String firebaseUid, String email, OffsetDateTime now) {
    if (firebaseUid == null || firebaseUid.isBlank()) {
      throw new IllegalArgumentException("firebaseUid is required");
    }
    if (email == null || email.isBlank()) {
      throw new IllegalArgumentException("email is required");
    }
    if (now == null) {
      throw new IllegalArgumentException("now is required");
    }

    return AppUser.builder()
        .firebaseUid(firebaseUid)
        .email(email.trim().toLowerCase())
        .role(Role.PATIENT)
        .enabled(true)
        .createdAt(now)
        .build();
  }

  /**
   * Legacy helper (avoid using in new code; not Clock-friendly).
   */
  @Deprecated(forRemoval = true)
  public static AppUser createNewPatient(String firebaseUid, String email) {
    return createNewPatient(firebaseUid, email, OffsetDateTime.now());
  }
}