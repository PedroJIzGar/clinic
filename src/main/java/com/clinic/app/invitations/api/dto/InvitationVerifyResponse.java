package com.clinic.app.invitations.api.dto;

import com.clinic.app.invitations.domain.InvitationStatus;

import java.time.OffsetDateTime;

public record InvitationVerifyResponse(
    boolean valid,
    InvitationStatus status,
    OffsetDateTime expiresAt
) {}