package com.clinic.app.users.api.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMyProfileRequest(
		@NotBlank @Size(max = 120) String fullName,
		@NotBlank @Size(max = 32) String phone) 
{}
