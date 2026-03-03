package com.clinic.app.config;

import java.io.FileInputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import jakarta.annotation.PostConstruct;

@Configuration
public class FirebaseConfig {

	@Value("${app.firebase.service-account-path}")
	private String serviceAccountPath;
	
	@PostConstruct
	public void init() throws Exception {
		if (serviceAccountPath == null || serviceAccountPath.isBlank()) {
			throw new IllegalStateException("Missing FIREBASE_SERVICE_ACCOUNT_PATH env var");
		}
		
		if (FirebaseApp.getApps().isEmpty()) {
		      try (FileInputStream serviceAccount = new FileInputStream(serviceAccountPath)) {
		          FirebaseOptions options = FirebaseOptions.builder()
		              .setCredentials(GoogleCredentials.fromStream(serviceAccount))
		              .build();
		          FirebaseApp.initializeApp(options);
		      }  
		}
    }
 }
