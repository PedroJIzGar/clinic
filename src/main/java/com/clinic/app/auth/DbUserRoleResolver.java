package com.clinic.app.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.clinic.app.invitations.domain.InvitationStatus;
import com.clinic.app.invitations.domain.StaffInvitation;
import com.clinic.app.invitations.repo.repository.StaffInvitationRepository;
import com.clinic.app.users.domain.AppUser;
import com.clinic.app.users.domain.Role;
import com.clinic.app.users.repo.repository.AppUserRepository;


@Service
public class DbUserRoleResolver implements UserRoleResolver {

  private final AppUserRepository userRepo;
  private final StaffInvitationRepository invitationRepo;

  public DbUserRoleResolver(AppUserRepository userRepo, StaffInvitationRepository invitationRepo) {
    this.userRepo = userRepo;
    this.invitationRepo = invitationRepo;
  }

  @Override
  @Transactional
  public Role resolveRole(String firebaseUid, String email) {
	  
    AppUser user = userRepo.findByFirebaseUid(firebaseUid).orElse(null);

    if (user == null) {
      // Si no hay user, miramos invitación por email (si viene null, lo tratamos como patient)
      var invOpt = (email == null || email.isBlank())
          ? java.util.Optional.<StaffInvitation>empty()
          : invitationRepo.findPendingByEmail(email);

      Role roleToAssign = invOpt.map(inv -> inv.getRole()).orElse(Role.PATIENT);

      user = userRepo.save(AppUser.builder()
          .firebaseUid(firebaseUid)
          .email(email == null ? "unknown" : email.trim().toLowerCase())
          .role(roleToAssign)
          .enabled(true)
          .createdAt(java.time.OffsetDateTime.now())
          .build());

      // Si había invitación, la marcamos como aceptada
      invOpt.ifPresent(inv -> {
        inv.setStatus(InvitationStatus.ACCEPTED);
        inv.setAcceptedAt(java.time.OffsetDateTime.now());
        invitationRepo.save(inv);
      });
    }

    if (!user.isEnabled()) {
      throw new IllegalStateException("User disabled");
    }

    return user.getRole();
  }
}