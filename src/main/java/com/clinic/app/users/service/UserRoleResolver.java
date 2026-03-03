package com.clinic.app.users.service;

import com.clinic.app.users.domain.Role;

public interface UserRoleResolver {

	Role resolveRole(String firebaseUid, String email);
	
}
