package com.clinic.app.shared.security;

import java.io.Serializable;

import com.clinic.app.users.domain.Role;

public record FirebasePrincipal(
    String uid,
    String email,
    Role role
) implements Serializable {}