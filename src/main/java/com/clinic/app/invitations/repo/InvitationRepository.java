package com.clinic.app.invitations.repo;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.clinic.app.invitations.domain.InvitationStatus;
import com.clinic.app.invitations.domain.Invitation;

public interface InvitationRepository extends JpaRepository<Invitation, Long>, JpaSpecificationExecutor<Invitation> {
	 
// --- Email-based (idempotency) ---

  Optional<Invitation> findByEmailAndStatus(String email, InvitationStatus status);

  default Optional<Invitation> findPendingByEmail(String normalizedEmail) {
    return findByEmailAndStatus(normalizedEmail, InvitationStatus.PENDING);
  }

  boolean existsByEmailAndStatus(String email, InvitationStatus status);

  default boolean existsPendingByEmail(String normalizedEmail) {
    return existsByEmailAndStatus(normalizedEmail, InvitationStatus.PENDING);
  }

  // --- Token-based (public accept/verify) ---

  Optional<Invitation> findByTokenHash(String tokenHash);

  Optional<Invitation> findByTokenHashAndStatus(String tokenHash, InvitationStatus status);

  default Optional<Invitation> findPendingByTokenHash(String tokenHash) {
    return findByTokenHashAndStatus(tokenHash, InvitationStatus.PENDING);
  }

  boolean existsByTokenHash(String tokenHash);

  // --- Expiration helpers (optional but handy) ---

  /**
   * True if there is a pending invitation for email that is still valid at 'now'.
   * Useful if you want to treat expired invitations as non-existing.
   */
  boolean existsByEmailAndStatusAndExpiresAtAfter(String email, InvitationStatus status, OffsetDateTime now);

  default boolean existsValidPendingByEmail(String normalizedEmail, OffsetDateTime now) {
    return existsByEmailAndStatusAndExpiresAtAfter(normalizedEmail, InvitationStatus.PENDING, now);
  }
}
