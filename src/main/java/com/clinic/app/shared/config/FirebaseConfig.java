package com.clinic.app.shared.config;

import java.io.FileInputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;

@Configuration
public class FirebaseConfig {

  @Value("${app.firebase.service-account-path}")
  private String serviceAccountPath;

  @Bean
  public FirebaseApp firebaseApp() throws Exception {
    if (serviceAccountPath == null || serviceAccountPath.isBlank()) {
      throw new IllegalStateException("Missing app.firebase.service-account-path");
    }

    // Reuse default app if already initialized (e.g. tests / reload)
    if (!FirebaseApp.getApps().isEmpty()) {
      return FirebaseApp.getInstance();
    }

    try (FileInputStream serviceAccount = new FileInputStream(serviceAccountPath)) {
      FirebaseOptions options = FirebaseOptions.builder()
          .setCredentials(GoogleCredentials.fromStream(serviceAccount))
          .build();

      return FirebaseApp.initializeApp(options);
    }
  }

  @Bean
  public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
    return FirebaseAuth.getInstance(firebaseApp);
  }
}