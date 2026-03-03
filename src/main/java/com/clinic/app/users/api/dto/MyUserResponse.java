package com.clinic.app.users.api.dto;

public record MyUserResponse(
	    String firebaseUid,
	    String email,
	    String role,
	    boolean profileCompleted
	) {}
