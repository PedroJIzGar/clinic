package com.clinic.app.invitations.api.controller;


import com.clinic.app.invitations.api.dto.CreateInvitationRequest;
import com.clinic.app.invitations.api.dto.InvitationPageResponse;
import com.clinic.app.invitations.api.dto.InvitationResponse;
import com.clinic.app.invitations.domain.InvitationStatus;
import com.clinic.app.invitations.service.InvitationService;
import com.clinic.app.shared.security.FirebasePrincipal;
import com.clinic.app.users.domain.Role;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/invitations")
public class AdminInvitationController {

  private final InvitationService invitationService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public InvitationResponse create(@Valid @RequestBody CreateInvitationRequest req, Authentication auth) {
    FirebasePrincipal principal = (FirebasePrincipal) auth.getPrincipal();
    return invitationService.create(req, principal);
  }

  @GetMapping
  public InvitationPageResponse list(
      @RequestParam(required = false) Role role,
      @RequestParam(required = false) InvitationStatus status,
      @RequestParam(required = false) String q,
      Pageable pageable
  ) {
    return invitationService.list(role, status, q, pageable);
  }

  @PostMapping("/{id}/resend")
  public InvitationResponse resend(@PathVariable Long id, Authentication auth) {
    FirebasePrincipal principal = (FirebasePrincipal) auth.getPrincipal();
    return invitationService.resend(id, principal);
  }

  @PatchMapping("/{id}/cancel")
  public InvitationResponse cancel(@PathVariable Long id, Authentication auth) {
    FirebasePrincipal principal = (FirebasePrincipal) auth.getPrincipal();
    return invitationService.cancel(id, principal);
  }
}