package com.clinic.app.invitations.api.controller;

import com.clinic.app.invitations.api.dto.AcceptInvitationRequest;
import com.clinic.app.invitations.api.dto.AcceptInvitationResponse;
import com.clinic.app.invitations.api.dto.InvitationVerifyResponse;
import com.clinic.app.invitations.domain.Invitation;
import com.clinic.app.invitations.service.InvitationService;
import com.clinic.app.shared.security.FirebasePrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/invitations")
public class PublicInvitationController {

  private final InvitationService invitationService;

  @GetMapping("/verify")
  public InvitationVerifyResponse verify(@RequestParam String token) {
    var r = invitationService.verify(token);
    return new InvitationVerifyResponse(r.valid(), r.status(), r.expiresAt());
  }

  @PostMapping("/accept")
  public AcceptInvitationResponse accept(@Valid @RequestBody AcceptInvitationRequest req, Authentication auth) {
    FirebasePrincipal principal = (FirebasePrincipal) auth.getPrincipal();

    Invitation inv = invitationService.accept(req.token(), principal);

    return new AcceptInvitationResponse(
        inv.getId(),
        inv.getEmail(),
        inv.getRole(),
        inv.getStatus(),
        inv.getAcceptedAt()
    );
  }
}