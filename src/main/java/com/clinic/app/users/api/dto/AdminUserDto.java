package com.clinic.app.users.api.dto;

import java.time.OffsetDateTime;

import com.clinic.app.users.domain.Role;

public record AdminUserDto(    
		Long id,
	    String email,
	    String firebaseUid,
	    Role role,
	    boolean enabled,
	    OffsetDateTime createdAt) {}
