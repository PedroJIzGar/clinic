package com.clinic.app.shared.security;

import com.clinic.app.users.domain.Role;

public interface UserRoleResolver {

	Role resolveRole(String firebaseUid, String email);
	
}
