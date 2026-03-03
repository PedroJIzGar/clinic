package com.clinic.app.invitations.repo.spec;

import org.springframework.data.jpa.domain.Specification;

import com.clinic.app.invitations.domain.InvitationStatus;
import com.clinic.app.invitations.domain.Invitation;
import com.clinic.app.users.domain.Role;

public class InvitationSpecs {

  public static Specification<Invitation> hasRole(Role role) {
    return (root, q, cb) -> cb.equal(root.get("role"), role);
  }

  public static Specification<Invitation> hasStatus(InvitationStatus status) {
    return (root, q, cb) -> cb.equal(root.get("status"), status);
  }

  public static Specification<Invitation> emailContains(String q) {
    return (root, query, cb) -> cb.like(cb.lower(root.get("email")), "%" + q.toLowerCase() + "%");
  }
}