package com.clinic.app.admin.controller;

import java.time.OffsetDateTime;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.clinic.app.admin.StaffInvitation;
import com.clinic.app.admin.dto.CreateInvitationRequest;
import com.clinic.app.admin.dto.InvitationResponse;
import com.clinic.app.admin.repository.StaffInvitationRepository;
import com.clinic.app.admin.spec.InvitationSpecs;
import com.clinic.app.common.exception.ConflictException;
import com.clinic.app.users.AppUser;
import com.clinic.app.users.InvitationStatus;
import com.clinic.app.users.Role;
import com.clinic.app.users.repository.AppUserRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/invitations")
@PreAuthorize("hasRole('ADMIN')")
public class AdminInvitationController {

  private static final Set<Role> ALLOWED = Set.of(Role.DENTIST, Role.RECEPTIONIST, Role.ADMIN);

  private final StaffInvitationRepository invitationRepo;
  private final AppUserRepository userRepo;

  public AdminInvitationController(StaffInvitationRepository invitationRepo, AppUserRepository userRepo) {
    this.invitationRepo = invitationRepo;
    this.userRepo = userRepo;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public InvitationResponse create(@Valid @RequestBody CreateInvitationRequest req, Authentication auth) {

    if (!ALLOWED.contains(req.role())) {
      throw new IllegalArgumentException("Role not allowed for invitation");
    }

    String email = req.email().trim().toLowerCase();

    invitationRepo.findPendingByEmail(email).ifPresent(i -> {
      throw new ConflictException("Invitation already pending for this email");
    });

    userRepo.findByEmailIgnoreCase(email).ifPresent(u -> {
    	  if (u.getRole() != Role.PATIENT) {
    	    throw new ConflictException("User already exists with staff role");
    	  }
    	});

    Long adminUserId = extractAdminUserId(auth);

    StaffInvitation inv = invitationRepo.save(StaffInvitation.builder()
        .email(req.email().trim().toLowerCase())
        .role(req.role())
        .status(InvitationStatus.PENDING)
        .createdByUserId(adminUserId)
        .createdAt(OffsetDateTime.now())
        .build());

    return new InvitationResponse(inv.getId(), inv.getEmail(), inv.getRole(), inv.getStatus(), inv.getCreatedAt());
  }
  
  @GetMapping
  public Page<InvitationResponse> list(
      @RequestParam(required = false) Role role,
      @RequestParam(required = false) InvitationStatus status,
      @RequestParam(required = false) String q,
      Pageable pageable
  ) {
	  Specification<StaffInvitation> spec = (root, query, cb) -> cb.conjunction();

    if (role != null) spec = spec.and(InvitationSpecs.hasRole(role));
    if (status != null) spec = spec.and(InvitationSpecs.hasStatus(status));
    if (q != null && !q.isBlank()) spec = spec.and(InvitationSpecs.emailContains(q));

    return invitationRepo.findAll(spec, pageable)
        .map(inv -> new InvitationResponse(inv.getId(), inv.getEmail(), inv.getRole(), inv.getStatus(), inv.getCreatedAt()));
  }

  private Long extractAdminUserId(Authentication auth) {
	  String firebaseUid = (String) auth.getPrincipal();
	  return userRepo.findByFirebaseUid(firebaseUid)
	      .map(AppUser::getId)
	      .orElseThrow(() -> new IllegalStateException("Admin user not found in DB"));
	}
}