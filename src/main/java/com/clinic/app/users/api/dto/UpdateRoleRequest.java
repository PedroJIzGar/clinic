package com.clinic.app.users.api.dto;

import com.clinic.app.users.domain.Role;

import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(
    @NotNull Role role
) {}