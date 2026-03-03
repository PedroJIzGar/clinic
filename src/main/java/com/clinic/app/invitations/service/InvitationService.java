package com.clinic.app.invitations.service;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.clinic.app.invitations.domain.Invitation;
import com.clinic.app.invitations.domain.InvitationStatus;
import com.clinic.app.invitations.repo.InvitationRepository;
import com.clinic.app.shared.security.FirebasePrincipal;
import com.clinic.app.shared.security.InvitationTokenService;
import com.clinic.app.users.domain.AppUser;
import com.clinic.app.users.domain.Role;

@Service
public class InvitationService {

    private final InvitationRepository invitationRepo;
    private final InvitationTokenService tokenService;
    private final InvitationEmailSender emailSender;
    private final AppUser userService;
    private final Clock clock;

    private final Duration ttl;
    private final Duration resendCooldown;
    private final String baseUrl;

    public InvitationService(
            InvitationRepository invitationRepo,
            InvitationTokenService tokenService,
            InvitationEmailSender emailSender,
            AppUser userService,
            Clock clock,
            @Value("${app.invitations.ttlHours:168}") long ttlHours,
            @Value("${app.invitations.resendCooldownSeconds:300}") long resendCooldownSeconds,
            @Value("${app.baseUrl:http://localhost:8080}") String baseUrl) {
        this.invitationRepo = invitationRepo;
        this.tokenService = tokenService;
        this.emailSender = emailSender;
        this.userService = userService;
        this.clock = clock;
        this.ttl = Duration.ofHours(ttlHours);
        this.resendCooldown = Duration.ofSeconds(resendCooldownSeconds);
        this.baseUrl = baseUrl;
    }

    /**
     * Creates a new PENDING invitation (token-based) OR returns the existing
     * PENDING if still valid.
     * - Idempotent: if PENDING + not expired => returns existing without resending.
     * - If PENDING but expired => marks EXPIRED and creates a new one.
     *
     * Token is NEVER returned in API response.
     * Token is delivered via InvitationEmailSender (DEV: logs + file).
     */
    @Transactional
    public Invitation createOrGetPending(String emailRaw, Role role, Long adminUserId) {
        Objects.requireNonNull(emailRaw, "email");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(adminUserId, "adminUserId");

        String email = normalizeEmail(emailRaw);
        OffsetDateTime now = OffsetDateTime.now(clock);

        // 1) If there is a PENDING invitation for this email, decide if it is still
        // valid
        var existingOpt = invitationRepo.findByEmailAndStatus(email, InvitationStatus.PENDING);

        if (existingOpt.isPresent()) {
            Invitation existing = existingOpt.get();

            if (isExpired(existing, now)) {
                existing.setStatus(InvitationStatus.EXPIRED);
                existing.setUpdatedAt(now);
                invitationRepo.save(existing);
            } else {
                // Idempotent: keep existing, do not generate new token, do not resend
                return existing;
            }
        }

        // 2) Optional: block inviting if email belongs to staff already etc.
        // Implement in userService if you want:
        // userService.assertInvitable(email, role);

        // 3) Create new token + store hash only
        String token = tokenService.generateToken();
        String tokenHash = tokenService.hashToken(token);

        Invitation inv = Invitation.builder()
                .email(email)
                .role(role)
                .status(InvitationStatus.PENDING)
                .createdByUserId(adminUserId)
                .createdAt(now)
                .updatedAt(now)
                .tokenHash(tokenHash)
                .expiresAt(now.plus(ttl))
                .lastSentAt(now)
                .sendCount(1)
                .build();

        Invitation saved = invitationRepo.save(inv);

        // 4) Deliver token via "email" (DEV: logs + file)
        String link = buildInvitationLink(token);
        emailSender.sendStaffInvitationEmail(email, link, saved.getExpiresAt());

        return saved;
    }

    /**
     * Resend: only for PENDING and not expired.
     * Regenerates token (recommended) and overwrites tokenHash.
     * Cooldown protected.
     */
    @Transactional
    public Invitation resend(Long invitationId, Long adminUserId) {
        Objects.requireNonNull(invitationId, "invitationId");
        Objects.requireNonNull(adminUserId, "adminUserId");

        OffsetDateTime now = OffsetDateTime.now(clock);

        Invitation inv = invitationRepo.findById(invitationId)
                .orElseThrow(() -> new NotFound("Invitation not found"));

        if (inv.getStatus() != InvitationStatus.PENDING) {
            throw new Conflict("Only PENDING invitations can be resent");
        }

        if (isExpired(inv, now)) {
            inv.setStatus(InvitationStatus.EXPIRED);
            inv.setUpdatedAt(now);
            invitationRepo.save(inv);
            throw new Gone("Invitation expired");
        }

        if (inv.getLastSentAt() != null) {
            Duration sinceLast = Duration.between(inv.getLastSentAt(), now);
            if (sinceLast.compareTo(resendCooldown) < 0) {
                throw new TooManyRequests("Resend cooldown not reached");
            }
        }

        String token = tokenService.generateToken();
        String tokenHash = tokenService.hashToken(token);

        inv.setTokenHash(tokenHash);
        inv.setLastSentAt(now);
        inv.setSendCount(inv.getSendCount() + 1);
        inv.setUpdatedAt(now);

        Invitation saved = invitationRepo.save(inv);

        String link = buildInvitationLink(token);
        emailSender.sendStaffInvitationEmail(inv.getEmail(), link, saved.getExpiresAt());

        return saved;
    }

    /**
     * Cancel/revoke: only PENDING.
     */
    @Transactional
    public Invitation cancel(Long invitationId, Long adminUserId) {
        Objects.requireNonNull(invitationId, "invitationId");
        Objects.requireNonNull(adminUserId, "adminUserId");

        OffsetDateTime now = OffsetDateTime.now(clock);

        Invitation inv = invitationRepo.findById(invitationId)
                .orElseThrow(() -> new NotFound("Invitation not found"));

        if (inv.getStatus() != InvitationStatus.PENDING) {
            throw new Conflict("Only PENDING invitations can be cancelled");
        }

        inv.setStatus(InvitationStatus.CANCELLED);
        inv.setRevokedAt(now);
        inv.setUpdatedAt(now);
        return invitationRepo.save(inv);
    }

    /**
     * Public verify by token. Returns minimal info.
     * Does not reveal email.
     */
    @Transactional(readOnly = true)
    public VerificationResult verify(String rawToken) {
        String token = Objects.requireNonNull(rawToken, "token").trim();
        if (token.isEmpty())
            throw new BadRequest("Missing invitation token");

        OffsetDateTime now = OffsetDateTime.now(clock);

        String hash = tokenService.hashToken(token);
        Invitation inv = invitationRepo.findByTokenHash(hash)
                .orElseThrow(() -> new NotFound("Invitation not found"));

        boolean expired = isExpired(inv, now);
        boolean pending = inv.getStatus() == InvitationStatus.PENDING;

        return new VerificationResult(pending && !expired, inv.getStatus(), inv.getExpiresAt());
    }

    /**
     * Accept invitation:
     * - token must exist
     * - status must be PENDING
     * - not expired
     * - FirebasePrincipal email must match invitation email
     * - create/enable staff user
     */
    @Transactional
    public Invitation accept(String rawToken, FirebasePrincipal principal) {
        Objects.requireNonNull(principal, "principal");

        String token = Objects.requireNonNull(rawToken, "token").trim();
        if (token.isEmpty())
            throw new BadRequest("Missing invitation token");

        OffsetDateTime now = OffsetDateTime.now(clock);

        String hash = tokenService.hashToken(token);
        Invitation inv = invitationRepo.findByTokenHash(hash)
                .orElseThrow(() -> new NotFound("Invitation not found"));

        if (inv.getStatus() != InvitationStatus.PENDING) {
            throw new Conflict("Invitation already used or not pending");
        }

        if (isExpired(inv, now)) {
            inv.setStatus(InvitationStatus.EXPIRED);
            inv.setUpdatedAt(now);
            invitationRepo.save(inv);
            throw new Gone("Invitation expired");
        }

        String principalEmail = normalizeEmail(principal.email());
        if (!principalEmail.equals(inv.getEmail())) {
            throw new Conflict("Authenticated email does not match invitation");
        }

        // Create or update staff user (you implement this method in UserService)
        userService.createOrUpdateStaffFromInvitation(principal.uid(), principal.email(), inv.getRole());

        inv.setStatus(InvitationStatus.ACCEPTED);
        inv.setAcceptedAt(now);
        inv.setUpdatedAt(now);
        return invitationRepo.save(inv);
    }

    // ---------- helpers ----------

    private String buildInvitationLink(String rawToken) {
        // You can point to a front-end route as well, e.g. baseUrl +
        // "/invite/accept?token=..."
        return baseUrl + "/invite/accept?token=" + rawToken;
    }

    private boolean isExpired(Invitation inv, OffsetDateTime now) {
        return inv.getExpiresAt() != null && inv.getExpiresAt().isBefore(now);
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    // ---------- service-level result ----------
    public record VerificationResult(boolean valid, InvitationStatus status, OffsetDateTime expiresAt) {
    }

    // ---------- minimal exceptions (replace with your shared ones if you have
    // them) ----------
    public static class BadRequest extends RuntimeException {
        public BadRequest(String m) {
            super(m);
        }
    }

    public static class NotFound extends RuntimeException {
        public NotFound(String m) {
            super(m);
        }
    }

    public static class Conflict extends RuntimeException {
        public Conflict(String m) {
            super(m);
        }
    }

    public static class Gone extends RuntimeException {
        public Gone(String m) {
            super(m);
        }
    }

    public static class TooManyRequests extends RuntimeException {
        public TooManyRequests(String m) {
            super(m);
        }
    }
}