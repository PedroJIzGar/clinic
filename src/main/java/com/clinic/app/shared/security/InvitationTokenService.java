package com.clinic.app.shared.security;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class InvitationTokenService {

  private static final String HMAC_ALG = "HmacSHA256";
  private static final int TOKEN_BYTES = 32; // 256 bits

  private final SecureRandom secureRandom = new SecureRandom();
  private final byte[] secretBytes;

  public InvitationTokenService(@Value("${app.invitations.tokenSecret:}") String tokenSecret) {
    if (tokenSecret == null || tokenSecret.isBlank()) {
      throw new IllegalArgumentException("Missing app.invitations.tokenSecret");
    }
    this.secretBytes = tokenSecret.getBytes(StandardCharsets.UTF_8);
  }

  /**
   * Returns a URL-safe random token (send to user).
   * Store ONLY the hash in DB.
   */
  public String generateToken() {
    byte[] randomBytes = new byte[TOKEN_BYTES];
    secureRandom.nextBytes(randomBytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
  }

  /**
   * Deterministic hash for lookup (HMAC(token, secret)).
   * Store this value in DB, not the raw token.
   */
  public String hashToken(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      throw new IllegalArgumentException("Missing token");
    }
    try {
      Mac mac = Mac.getInstance(HMAC_ALG);
      mac.init(new SecretKeySpec(secretBytes, HMAC_ALG));
      byte[] digest = mac.doFinal(rawToken.trim().getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to hash invitation token", e);
    }
  }
}