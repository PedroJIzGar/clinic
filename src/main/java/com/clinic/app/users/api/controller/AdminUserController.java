package com.clinic.app.users.api.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.clinic.app.shared.exception.ConflictException;
import com.clinic.app.shared.exception.NotFoundException;
import com.clinic.app.users.api.dto.AdminUserResponse;
import com.clinic.app.users.api.dto.EnableUserRequest;
import com.clinic.app.users.api.dto.UpdateRoleRequest;
import com.clinic.app.users.domain.AppUser;
import com.clinic.app.users.domain.Role;
import com.clinic.app.users.repo.AppUserRepository;
import com.clinic.app.users.repo.spec.AppUserSpecs;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

  private final AppUserRepository userRepo;

  public AdminUserController(AppUserRepository userRepo) {
    this.userRepo = userRepo;
  }

  @GetMapping
  public Page<AdminUserResponse> list(
      @RequestParam(required = false) Role role,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) Boolean enabled,
      Pageable pageable
  ) {
	  Specification<AppUser> spec = (root, query, cb) -> cb.conjunction();

    if (role != null) spec = spec.and(AppUserSpecs.hasRole(role));
    if (enabled != null) spec = spec.and(AppUserSpecs.isEnabled(enabled));
    if (q != null && !q.isBlank()) spec = spec.and(AppUserSpecs.emailContains(q));

    return userRepo.findAll(spec, pageable).map(AdminUserResponse::from);
  }
  
  @PatchMapping("/{id}/role")
  public AdminUserResponse updateRole(@PathVariable Long id, @Valid @RequestBody UpdateRoleRequest req) {

    AppUser user = userRepo.findById(id)
        .orElseThrow(() -> new NotFoundException("User not found: " + id));

    // Evitar dejar el sistema sin admins
    if (user.getRole() == Role.ADMIN && req.role() != Role.ADMIN) {
      long adminsEnabled = userRepo.countByRoleAndEnabledTrue(Role.ADMIN);
      if (adminsEnabled <= 1) {
        throw new ConflictException("Cannot remove the last enabled ADMIN");
      }
    }

    user.setRole(req.role());
    userRepo.save(user);
    return AdminUserResponse.from(user);
  }

  @PatchMapping("/{id}/enable")
  public AdminUserResponse setEnabled(@PathVariable Long id, @Valid @RequestBody EnableUserRequest req) {

    AppUser user = userRepo.findById(id)
        .orElseThrow(() -> new NotFoundException("User not found: " + id));

    // Evitar deshabilitar el último admin habilitado
    if (user.getRole() == Role.ADMIN && Boolean.FALSE.equals(req.enabled())) {
      long adminsEnabled = userRepo.countByRoleAndEnabledTrue(Role.ADMIN);
      if (adminsEnabled <= 1) {
        throw new ConflictException("Cannot disable the last enabled ADMIN");
      }
    }

    user.setEnabled(req.enabled());
    userRepo.save(user);
    return AdminUserResponse.from(user);
  }
}