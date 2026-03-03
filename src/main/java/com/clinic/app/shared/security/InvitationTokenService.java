package com.clinic.app.shared.security;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

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

    public InvitationTokenService(@Value("${app.invitation.token.secret:}") String tokenSecret) {
        if (tokenSecret == null || tokenSecret.isBlank()) {
            throw new IllegalArgumentException("Missing property: app.invitation.token.secret");
        }
        this.secretBytes = tokenSecret.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Returns a URL-safe token to be delivered to the invited staff member.
     * Store only the hash in DB.
     */
    public String generateToken() {
        byte[] randomBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Hash token using HMAC-SHA256 with a server secret.
     * Deterministic: same token => same hash.
     */
    public String hashToken(String token) {
        Objects.requireNonNull(token, "token");
        try {
            Mac mac = Mac.getInstance(HMAC_ALG);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretBytes, HMAC_ALG);
            mac.init(secretKeySpec);
            byte[] hashBytes = mac.doFinal(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error hashing token", e);
        }
    }

    /**
     * Constant-time comparison of expected vs computed hash.
     */
    public boolean matches(String token, String expectedHash) {
        if (token == null || expectedHash == null)
            return false;
        String computed = hashToken(token);
        return constantTimeEquals(computed, expectedHash);
    }

    private boolean constantTimeEquals(String a, String b) {
        byte[] x = a.getBytes(StandardCharsets.UTF_8);
        byte[] y = b.getBytes(StandardCharsets.UTF_8);
        if (x.length != y.length)
            return false;
        int result = 0;
        for (int i = 0; i < x.length; i++) {
            result |= x[i] ^ y[i];
        }
        return result == 0;
    }
}
