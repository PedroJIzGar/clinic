package com.clinic.app.users.api.dtos;

public record MyUserResponse(
	    String firebaseUid,
	    String email,
	    String role,
	    boolean profileCompleted
	) {}
