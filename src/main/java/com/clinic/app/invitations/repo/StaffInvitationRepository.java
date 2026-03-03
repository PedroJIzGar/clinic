package com.clinic.app.invitations.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.clinic.app.invitations.domain.InvitationStatus;
import com.clinic.app.invitations.domain.StaffInvitation;

public interface StaffInvitationRepository extends JpaRepository<StaffInvitation, Long>, JpaSpecificationExecutor<StaffInvitation> {
	 
  Optional<StaffInvitation> findByEmailIgnoreCaseAndStatus(String email, InvitationStatus status);

  default Optional<StaffInvitation> findPendingByEmail(String email) {
    return findByEmailIgnoreCaseAndStatus(email, InvitationStatus.PENDING);
  }
}
