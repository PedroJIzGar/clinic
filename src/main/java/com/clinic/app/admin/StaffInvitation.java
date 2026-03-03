package com.clinic.app.admin;

import java.time.OffsetDateTime;

import com.clinic.app.users.InvitationStatus;
import com.clinic.app.users.Role;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "staff_invitation")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class StaffInvitation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 255, unique = true)
  private String email;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private Role role;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private InvitationStatus status;

  @Column(name = "created_by_user_id", nullable = false)
  private Long createdByUserId;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "accepted_at")
  private OffsetDateTime acceptedAt;

  public void accept() {
    this.status = InvitationStatus.ACCEPTED;
    this.acceptedAt = OffsetDateTime.now();
  }

  public void cancel() {
    this.status = InvitationStatus.CANCELLED;
  }
}