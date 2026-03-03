package com.clinic.app.invitations.api.dtos;

import com.clinic.app.users.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record CreateInvitationRequest(
		@Email @NotNull String email,
	    @NotNull Role role
	    ) {}
