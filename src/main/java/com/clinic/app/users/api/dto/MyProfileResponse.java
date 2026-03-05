package com.clinic.app.users.api.dto;

import java.time.OffsetDateTime;

public record MyProfileResponse(
	String fullName, 
	String phone, 
	OffsetDateTime updatedAt) {}
