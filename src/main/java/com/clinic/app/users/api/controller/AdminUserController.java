package com.clinic.app.users.api.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.clinic.app.users.api.dto.AdminUserResponse;
import com.clinic.app.users.api.dto.EnableUserRequest;
import com.clinic.app.users.api.dto.UpdateRoleRequest;
import com.clinic.app.users.domain.Role;
import com.clinic.app.users.service.AdminUserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

  private final AdminUserService adminUserService;

  public AdminUserController(AdminUserService adminUserService) {
    this.adminUserService = adminUserService;
  }

  @GetMapping
  public Page<AdminUserResponse> list(
      @RequestParam(required = false) Role role,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) Boolean enabled,
      Pageable pageable
  ) {
    return adminUserService.list(role, q, enabled, pageable)
        .map(AdminUserResponse::from);
  }

  @PatchMapping("/{id}/role")
  public AdminUserResponse updateRole(@PathVariable Long id, @Valid @RequestBody UpdateRoleRequest req) {
    return AdminUserResponse.from(adminUserService.updateRole(id, req.role()));
  }

  @PatchMapping("/{id}/enable")
  public AdminUserResponse setEnabled(@PathVariable Long id, @Valid @RequestBody EnableUserRequest req) {
    return AdminUserResponse.from(adminUserService.setEnabled(id, req.enabled()));
  }
}