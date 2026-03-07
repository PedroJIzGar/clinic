package com.clinic.app.invitations.service;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.clinic.app.invitations.api.dto.CreateInvitationRequest;
import com.clinic.app.invitations.api.dto.InvitationPageResponse;
import com.clinic.app.invitations.api.dto.InvitationResponse;
import com.clinic.app.invitations.domain.Invitation;
import com.clinic.app.invitations.domain.InvitationStatus;
import com.clinic.app.invitations.repo.InvitationRepository;
import com.clinic.app.invitations.repo.spec.InvitationSpecs;
import com.clinic.app.shared.exception.ConflictException;
import com.clinic.app.shared.exception.NotFoundException;
import com.clinic.app.shared.security.FirebasePrincipal;
import com.clinic.app.shared.security.InvitationTokenService;
import com.clinic.app.users.domain.Role;
import com.clinic.app.users.service.CurrentUserService;
import com.clinic.app.users.service.UserProvisioningService;

@Service
public class InvitationService {

  private final InvitationRepository invitationRepo;
  private final InvitationTokenService tokenService;
  private final InvitationEmailSender emailSender;
  private final UserProvisioningService userProvisioningService;
  private final CurrentUserService currentUserService;
  private final Clock clock;

  private final Duration ttl;
  private final Duration resendCooldown;
  private final String baseUrl;

  public InvitationService(
      InvitationRepository invitationRepo,
      InvitationTokenService tokenService,
      InvitationEmailSender emailSender,
      UserProvisioningService userProvisioningService,
      CurrentUserService currentUserService,
      Clock clock,
      @Value("${app.invitations.ttlHours:168}") long ttlHours,
      @Value("${app.invitations.resendCooldownSeconds:300}") long resendCooldownSeconds,
      @Value("${app.baseUrl:http://localhost:8080}") String baseUrl) {

    this.invitationRepo = invitationRepo;
    this.tokenService = tokenService;
    this.emailSender = emailSender;
    this.userProvisioningService = userProvisioningService;
    this.currentUserService = currentUserService;
    this.clock = clock;

    this.ttl = Duration.ofHours(ttlHours);
    this.resendCooldown = Duration.ofSeconds(resendCooldownSeconds);
    this.baseUrl = baseUrl;
  }

  // ---------------------------------------------------------------------------
  // API wrappers (AdminInvitationController stays thin)
  // ---------------------------------------------------------------------------

  @Transactional
  public InvitationResponse create(CreateInvitationRequest req, FirebasePrincipal principal) {
    Objects.requireNonNull(req, "req");
    Objects.requireNonNull(principal, "principal");

    Long adminUserId = currentUserService.requireAdminUserId();
    Invitation inv = createOrGetPending(req.email(), req.role(), adminUserId);
    return toResponse(inv);
  }

  @Transactional(readOnly = true)
  public InvitationPageResponse list(Role role, InvitationStatus status, String q, Pageable pageable) {
    Specification<Invitation> spec = (root, query, cb) -> cb.conjunction();

    if (role != null) spec = spec.and(InvitationSpecs.hasRole(role));
    if (status != null) spec = spec.and(InvitationSpecs.hasStatus(status));
    if (q != null && !q.isBlank()) spec = spec.and(InvitationSpecs.emailContains(q));

    Page<InvitationResponse> page = invitationRepo.findAll(spec, pageable).map(this::toResponse);

    return new InvitationPageResponse(
        page.getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.isFirst(),
        page.isLast()
    );
  }

  @Transactional
  public InvitationResponse resend(Long invitationId, FirebasePrincipal principal) {
    Objects.requireNonNull(principal, "principal");

    Long adminUserId = currentUserService.requireAdminUserId();
    Invitation inv = resendInternal(invitationId, adminUserId);
    return toResponse(inv);
  }

  @Transactional
  public InvitationResponse cancel(Long invitationId, FirebasePrincipal principal) {
    Objects.requireNonNull(principal, "principal");

    Long adminUserId = currentUserService.requireAdminUserId();
    Invitation inv = cancelInternal(invitationId, adminUserId);
    return toResponse(inv);
  }

  // ---------------------------------------------------------------------------
  // Core domain methods
  // ---------------------------------------------------------------------------

  /**
   * Creates a new PENDING invitation OR returns existing PENDING if still valid.
   * - If PENDING exists and not expired -> idempotent return (no new token, no resend)
   * - If PENDING exists but expired -> mark EXPIRED and create new
   *
   * Token is delivered via InvitationEmailSender (DEV: logs + file).
   */
  @Transactional
  public Invitation createOrGetPending(String emailRaw, Role role, Long adminUserId) {
    Objects.requireNonNull(emailRaw, "email");
    Objects.requireNonNull(role, "role");
    Objects.requireNonNull(adminUserId, "adminUserId");

    String email = normalizeEmail(emailRaw);
    if (email.isBlank()) throw new IllegalArgumentException("Email is required");

    OffsetDateTime now = OffsetDateTime.now(clock);

    var existingOpt = invitationRepo.findByEmailAndStatus(email, InvitationStatus.PENDING);
    if (existingOpt.isPresent()) {
      Invitation existing = existingOpt.get();

      if (isExpired(existing, now)) {
        existing.setStatus(InvitationStatus.EXPIRED);
        existing.setUpdatedAt(now);
        invitationRepo.save(existing);
      } else {
        return existing;
      }
    }

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

    String link = buildInvitationLink(token);
    emailSender.sendStaffInvitationEmail(email, link, saved.getExpiresAt());

    return saved;
  }

  /**
   * Resend: only PENDING and not expired. Cooldown protected.
   * Rotates tokenHash by generating a new token.
   */
  @Transactional
  public Invitation resendInternal(Long invitationId, Long adminUserId) {
    Objects.requireNonNull(invitationId, "invitationId");
    Objects.requireNonNull(adminUserId, "adminUserId");

    OffsetDateTime now = OffsetDateTime.now(clock);

    Invitation inv = invitationRepo.findById(invitationId)
        .orElseThrow(() -> new NotFoundException("Invitation not found"));

    if (inv.getStatus() != InvitationStatus.PENDING) {
      throw new ConflictException("Only PENDING invitations can be resent");
    }

    if (isExpired(inv, now)) {
      inv.setStatus(InvitationStatus.EXPIRED);
      inv.setUpdatedAt(now);
      invitationRepo.save(inv);
      throw new ConflictException("Invitation expired");
    }

    if (inv.getLastSentAt() != null) {
      Duration sinceLast = Duration.between(inv.getLastSentAt(), now);
      if (sinceLast.compareTo(resendCooldown) < 0) {
        throw new ConflictException("Resend cooldown not reached");
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
  public Invitation cancelInternal(Long invitationId, Long adminUserId) {
    Objects.requireNonNull(invitationId, "invitationId");
    Objects.requireNonNull(adminUserId, "adminUserId");

    OffsetDateTime now = OffsetDateTime.now(clock);

    Invitation inv = invitationRepo.findById(invitationId)
        .orElseThrow(() -> new NotFoundException("Invitation not found"));

    if (inv.getStatus() != InvitationStatus.PENDING) {
      throw new ConflictException("Only PENDING invitations can be cancelled");
    }

    inv.setStatus(InvitationStatus.CANCELLED);
    inv.setRevokedAt(now);
    inv.setUpdatedAt(now);
    return invitationRepo.save(inv);
  }

  /**
   * Public verify by token. Minimal info, does not reveal email.
   */
  @Transactional(readOnly = true)
  public VerificationResult verify(String rawToken) {
    String token = Objects.requireNonNull(rawToken, "token").trim();
    if (token.isEmpty()) throw new IllegalArgumentException("Missing invitation token");

    OffsetDateTime now = OffsetDateTime.now(clock);

    String hash = tokenService.hashToken(token);
    Invitation inv = invitationRepo.findByTokenHash(hash)
        .orElseThrow(() -> new NotFoundException("Invitation not found"));

    boolean expired = isExpired(inv, now);
    boolean pending = inv.getStatus() == InvitationStatus.PENDING;

    return new VerificationResult(pending && !expired, inv.getStatus(), inv.getExpiresAt());
  }

  /**
   * Accept invitation:
   * - token exists
   * - status PENDING
   * - not expired
   * - FirebasePrincipal.email matches invitation.email
   * - create/enable staff user
   */
  @Transactional
  public Invitation accept(String rawToken, FirebasePrincipal principal) {
    Objects.requireNonNull(principal, "principal");

    String token = Objects.requireNonNull(rawToken, "token").trim();
    if (token.isEmpty()) throw new IllegalArgumentException("Missing invitation token");

    OffsetDateTime now = OffsetDateTime.now(clock);

    String hash = tokenService.hashToken(token);
    Invitation inv = invitationRepo.findByTokenHash(hash)
        .orElseThrow(() -> new NotFoundException("Invitation not found"));

    if (inv.getStatus() != InvitationStatus.PENDING) {
      throw new ConflictException("Invitation already used or not pending");
    }

    if (isExpired(inv, now)) {
      inv.setStatus(InvitationStatus.EXPIRED);
      inv.setUpdatedAt(now);
      invitationRepo.save(inv);
      throw new ConflictException("Invitation expired");
    }

    String principalEmail = normalizeEmail(principal.email());
    if (!principalEmail.equals(inv.getEmail())) {
      throw new ConflictException("Authenticated email does not match invitation");
    }

    userProvisioningService.createOrUpdateStaffFromInvitation(
        principal.uid(),
        principal.email(),
        inv.getRole()
    );

    inv.setStatus(InvitationStatus.ACCEPTED);
    inv.setAcceptedAt(now);
    inv.setUpdatedAt(now);
    return invitationRepo.save(inv);
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private InvitationResponse toResponse(Invitation inv) {
    return new InvitationResponse(inv.getId(), inv.getEmail(), inv.getRole(), inv.getStatus(), inv.getCreatedAt());
  }

  private String buildInvitationLink(String rawToken) {
    return baseUrl + "/invite/accept?token=" + rawToken;
  }

  private boolean isExpired(Invitation inv, OffsetDateTime now) {
    return inv.getExpiresAt() != null && inv.getExpiresAt().isBefore(now);
  }

  private String normalizeEmail(String email) {
    return email == null ? "" : email.trim().toLowerCase();
  }

  public record VerificationResult(boolean valid, InvitationStatus status, OffsetDateTime expiresAt) {}
}