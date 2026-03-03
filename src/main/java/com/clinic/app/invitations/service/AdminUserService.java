package com.clinic.app.invitations.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.clinic.app.invitations.api.dtos.AdminUserDto;
import com.clinic.app.users.repo.repository.AppUserRepository;

@Service
public class AdminUserService {

  private final AppUserRepository userRepo;

  public AdminUserService(AppUserRepository userRepo) {
    this.userRepo = userRepo;
  }

  public Page<AdminUserDto> listUsers(Pageable pageable) {
    return userRepo.findAllByOrderByCreatedAtDesc(pageable)
        .map(u -> new AdminUserDto(
            u.getId(),
            u.getEmail(),
            u.getFirebaseUid(),
            u.getRole(),
            u.isEnabled(),
            u.getCreatedAt()
        ));
  }
}