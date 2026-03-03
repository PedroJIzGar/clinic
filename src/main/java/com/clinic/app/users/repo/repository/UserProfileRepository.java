package com.clinic.app.users.repo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.clinic.app.users.domain.UserProfile;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {}
