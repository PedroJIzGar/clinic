package com.clinic.app.users.service;

import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.clinic.app.shared.exception.ConflictException;
import com.clinic.app.shared.exception.NotFoundException;
import com.clinic.app.users.domain.AppUser;
import com.clinic.app.users.domain.Role;
import com.clinic.app.users.repo.AppUserRepository;
import com.clinic.app.users.repo.spec.AppUserSpecs;

@Service
public class AdminUserService {

  private final AppUserRepository userRepo;

  public AdminUserService(AppUserRepository userRepo) {
    this.userRepo = userRepo;
  }

  @Transactional(readOnly = true)
  public Page<AppUser> list(Role role, String q, Boolean enabled, Pageable pageable) {
    Specification<AppUser> spec = (root, query, cb) -> cb.conjunction();

    if (role != null) spec = spec.and(AppUserSpecs.hasRole(role));
    if (enabled != null) spec = spec.and(AppUserSpecs.isEnabled(enabled));
    if (q != null && !q.isBlank()) spec = spec.and(AppUserSpecs.emailContains(q));

    return userRepo.findAll(spec, pageable);
  }

  @Transactional
  public AppUser updateRole(Long id, Role newRole) {
    Objects.requireNonNull(newRole, "newRole");

    AppUser user = userRepo.findById(id)
        .orElseThrow(() -> new NotFoundException("User not found: " + id));

    // Evitar dejar el sistema sin admins habilitados
    if (user.getRole() == Role.ADMIN && newRole != Role.ADMIN) {
      long adminsEnabled = userRepo.countByRoleAndEnabledTrue(Role.ADMIN);
      if (adminsEnabled <= 1) {
        throw new ConflictException("Cannot remove the last enabled ADMIN");
      }
    }

    if (user.getRole() == newRole) {
      return user; // no-op
    }

    user.setRole(newRole);
    return userRepo.save(user);
  }

  @Transactional
  public AppUser setEnabled(Long id, boolean enabled) {
    AppUser user = userRepo.findById(id)
        .orElseThrow(() -> new NotFoundException("User not found: " + id));

    // Evitar deshabilitar el último admin habilitado
    if (user.getRole() == Role.ADMIN && !enabled) {
      long adminsEnabled = userRepo.countByRoleAndEnabledTrue(Role.ADMIN);
      if (adminsEnabled <= 1) {
        throw new ConflictException("Cannot disable the last enabled ADMIN");
      }
    }

    if (user.isEnabled() == enabled) {
      return user; // no-op
    }

    user.setEnabled(enabled);
    return userRepo.save(user);
  }
}