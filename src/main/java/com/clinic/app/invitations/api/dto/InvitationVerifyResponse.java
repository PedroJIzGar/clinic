package com.clinic.app.invitations.api.dto;

import java.time.OffsetDateTime;
import com.clinic.app.invitations.domain.InvitationStatus;

public record InvitationVerifyResponse(
    boolean valid,
    InvitationStatus status,
    OffsetDateTime expiresAt
) {}