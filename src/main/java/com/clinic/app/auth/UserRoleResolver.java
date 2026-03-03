package com.clinic.app.auth;

import com.clinic.app.users.domain.Role;

public interface UserRoleResolver {

	Role resolveRole(String firebaseUid, String email);
	
}
