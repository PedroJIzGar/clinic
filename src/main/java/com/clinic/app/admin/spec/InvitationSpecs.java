package com.clinic.app.admin.spec;

import org.springframework.data.jpa.domain.Specification;
import com.clinic.app.admin.StaffInvitation;
import com.clinic.app.users.InvitationStatus;
import com.clinic.app.users.Role;

public class InvitationSpecs {

  public static Specification<StaffInvitation> hasRole(Role role) {
    return (root, q, cb) -> cb.equal(root.get("role"), role);
  }

  public static Specification<StaffInvitation> hasStatus(InvitationStatus status) {
    return (root, q, cb) -> cb.equal(root.get("status"), status);
  }

  public static Specification<StaffInvitation> emailContains(String q) {
    return (root, query, cb) -> cb.like(cb.lower(root.get("email")), "%" + q.toLowerCase() + "%");
  }
}