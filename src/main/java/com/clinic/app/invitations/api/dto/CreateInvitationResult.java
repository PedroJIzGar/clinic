package com.clinic.app.invitations.api.dto;

import com.clinic.app.invitations.domain.InvitationStatus;
import com.clinic.app.users.domain.Role;

import java.time.OffsetDateTime;

public record CreateInvitationResult(
    Long id,
    String email,
    Role role,
    InvitationStatus status,
    OffsetDateTime expiresAt,
    String rawToken // solo para desarrollo; en prod NO lo devuelvas
) {}