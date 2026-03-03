package com.clinic.app.users.repo;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.clinic.app.users.domain.AppUser;
import com.clinic.app.users.domain.Role;

public interface AppUserRepository extends JpaRepository<AppUser, Long>, JpaSpecificationExecutor<AppUser> {

  Optional<AppUser> findByFirebaseUid(String firebaseUid);

  Optional<AppUser> findByEmailIgnoreCase(String email);

  boolean existsByEmailIgnoreCase(String email);

  Page<AppUser> findAllByOrderByCreatedAtDesc(Pageable pageable);

  long countByRoleAndEnabledTrue(Role role);
}