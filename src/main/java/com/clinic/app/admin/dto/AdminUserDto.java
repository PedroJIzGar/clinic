package com.clinic.app.admin.dto;

import java.time.OffsetDateTime;

import com.clinic.app.users.Role;

public record AdminUserDto(    
		Long id,
	    String email,
	    String firebaseUid,
	    Role role,
	    boolean enabled,
	    OffsetDateTime createdAt) {}
