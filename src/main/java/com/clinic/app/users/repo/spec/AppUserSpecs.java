package com.clinic.app.users.repo.spec;

import org.springframework.data.jpa.domain.Specification;

import com.clinic.app.users.AppUser;
import com.clinic.app.users.Role;

public final class AppUserSpecs {

  private AppUserSpecs() {}

  public static Specification<AppUser> hasRole(Role role) {
    return (root, query, cb) -> cb.equal(root.get("role"), role);
  }

  public static Specification<AppUser> isEnabled(Boolean enabled) {
    return (root, query, cb) -> cb.equal(root.get("enabled"), enabled);
  }

  public static Specification<AppUser> emailContains(String q) {
    String like = "%" + q.toLowerCase().trim() + "%";
    return (root, query, cb) -> cb.like(cb.lower(root.get("email")), like);
  }
}