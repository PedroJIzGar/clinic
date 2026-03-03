package com.clinic.app.users;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_profile")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserProfile {

  @Id
  private Long userId;

  @Column(nullable = false, length = 120)
  private String fullName;

  @Column(nullable = false, length = 32)
  private String phone;

  @Column(nullable = false)
  private OffsetDateTime updatedAt;

  public void update(String fullName, String phone, OffsetDateTime updatedAt) {
    this.fullName = fullName;
    this.phone = phone;
    this.updatedAt = updatedAt;
  }
}