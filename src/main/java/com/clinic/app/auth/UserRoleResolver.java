package com.clinic.app.auth;

import com.clinic.app.users.Role;

public interface UserRoleResolver {

	Role resolveRole(String firebaseUid, String email);
	
}
