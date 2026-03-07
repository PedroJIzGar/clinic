package com.clinic.app.invitations.domain;

import java.time.OffsetDateTime;

import com.clinic.app.users.domain.Role;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "staff_invitations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invitation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 255)
  private String email;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private Role role;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private InvitationStatus status = InvitationStatus.PENDING;

  @Column(name = "created_by_user_id", nullable = false)
  private Long createdByUserId;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "accepted_at")
  private OffsetDateTime acceptedAt;

  @Column(name = "token_hash", length = 128)
  private String tokenHash;

  @Column(name = "expires_at")
  private OffsetDateTime expiresAt;

  @Column(name = "last_sent_at")
  private OffsetDateTime lastSentAt;

  @Builder.Default
  @Column(name = "send_count", nullable = false)
  private int sendCount = 0;

  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;

  @Column(name = "revoked_at")
  private OffsetDateTime revokedAt;

  @Version
  private Long version;

  public void markExpired(OffsetDateTime now) {
    requireNow(now);
    if (this.status != InvitationStatus.PENDING) {
      throw new IllegalStateException("Only pending invitations can be marked as expired");
    }
    this.status = InvitationStatus.EXPIRED;
    this.updatedAt = now;
  }

  public void resend(String newTokenHash, OffsetDateTime now) {
    requireNow(now);

    if (this.status != InvitationStatus.PENDING) {
      throw new IllegalStateException("Only pending invitations can be resent");
    }
    if (newTokenHash == null || newTokenHash.isBlank()) {
      throw new IllegalArgumentException("newTokenHash must not be blank");
    }

    this.tokenHash = newTokenHash;
    this.lastSentAt = now;
    this.sendCount = this.sendCount + 1;
    this.updatedAt = now;
  }

  public void accept(OffsetDateTime now) {
    requireNow(now);

    if (this.status != InvitationStatus.PENDING) {
      throw new IllegalStateException("Only pending invitations can be accepted");
    }

    this.status = InvitationStatus.ACCEPTED;
    this.acceptedAt = now;
    this.updatedAt = now;
  }

  public void cancel(OffsetDateTime now) {
    requireNow(now);

    if (this.status != InvitationStatus.PENDING) {
      throw new IllegalStateException("Only pending invitations can be cancelled");
    }

    this.status = InvitationStatus.CANCELLED;
    this.revokedAt = now;
    this.updatedAt = now;
  }

  private void requireNow(OffsetDateTime now) {
    if (now == null) {
      throw new IllegalArgumentException("now must not be null");
    }
  }
}