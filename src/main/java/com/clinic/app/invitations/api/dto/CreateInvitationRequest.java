package com.clinic.app.invitations.api.dto;

import com.clinic.app.users.domain.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record CreateInvitationRequest(
		@Email @NotNull String email,
	    @NotNull Role role
	    ) {}
