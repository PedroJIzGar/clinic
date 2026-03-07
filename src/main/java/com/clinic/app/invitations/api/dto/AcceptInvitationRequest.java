package com.clinic.app.invitations.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AcceptInvitationRequest(
    @NotBlank String token
) {}