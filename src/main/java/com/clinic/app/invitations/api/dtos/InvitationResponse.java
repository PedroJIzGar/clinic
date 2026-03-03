package com.clinic.app.invitations.api.dtos;

import java.time.OffsetDateTime;

import com.clinic.app.invitations.domain.InvitationStatus;
import com.clinic.app.users.domain.Role;

public record InvitationResponse(
	    Long id,
	    String email,
	    Role role,
	    InvitationStatus status,
	    OffsetDateTime createdAt
	    ) {}
