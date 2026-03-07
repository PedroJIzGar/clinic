package com.clinic.app.invitations.api.dto;

import java.time.OffsetDateTime;

import com.clinic.app.invitations.domain.InvitationStatus;
import com.clinic.app.users.domain.Role;

public record AcceptInvitationResponse(
    Long invitationId,
    String email,
    Role role,
    InvitationStatus status,
    OffsetDateTime acceptedAt
) {}