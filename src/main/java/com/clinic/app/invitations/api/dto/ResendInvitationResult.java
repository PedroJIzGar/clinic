package com.clinic.app.invitations.api.dto;

import com.clinic.app.invitations.domain.InvitationStatus;
import com.clinic.app.users.domain.Role;

import java.time.OffsetDateTime;

public record ResendInvitationResult(
    Long id,
    String email,
    Role role,
    InvitationStatus status,
    OffsetDateTime expiresAt,
    int sendCount,
    OffsetDateTime lastSentAt,
    String rawToken // solo para desarrollo
) {}