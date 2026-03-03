package com.clinic.app.invitations.api.dtos;

import java.time.OffsetDateTime;

import com.clinic.app.users.InvitationStatus;
import com.clinic.app.users.Role;

public record InvitationResponse(
	    Long id,
	    String email,
	    Role role,
	    InvitationStatus status,
	    OffsetDateTime createdAt
	    ) {}
