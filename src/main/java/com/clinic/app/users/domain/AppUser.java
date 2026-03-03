package com.clinic.app.users.domain;

import java.time.OffsetDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "app_user")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AppUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name = "firebase_uid", nullable = false, unique = true, length = 128)
	private String firebaseUid;
	
	@Column(nullable = false, unique = true)
	private String email;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private Role role;
	
	@Column(nullable = false)
	private boolean enabled;
	
	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;
	
	public static AppUser createNewPatient(String firebaseUid, String email) {
		return AppUser.builder()
				.firebaseUid(firebaseUid)
				.email(email == null ? "unknown" : email)
				.role(Role.PATIENT)
				.enabled(true)
				.createdAt(OffsetDateTime.now())
				.build();
	}

	
}
