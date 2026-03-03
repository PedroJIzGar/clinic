package com.clinic.app.users.dtos;

public record MyUserResponse(
	    String firebaseUid,
	    String email,
	    String role,
	    boolean profileCompleted
	) {}
