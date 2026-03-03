package com.clinic.app.users.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.clinic.app.users.UserProfile;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {}
