package com.clinic.app.invitations.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevInvitationEmailSender implements InvitationEmailSender {

    private static final Logger log = LoggerFactory.getLogger(DevInvitationEmailSender.class);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private final Path outputDir;
    private final Clock clock;
    private final boolean logFullInvitationLink;

    public DevInvitationEmailSender(
            @Value("${app.invitations.dev-mail.output-dir:dev-mails}") String outputDir,
            @Value("${app.invitations.dev-mail.log-full-link:false}") boolean logFullInvitationLink,
            Clock clock) {
        this.outputDir = Path.of(outputDir);
        this.logFullInvitationLink = logFullInvitationLink;
        this.clock = clock;
    }

    @Override
    public void sendStaffInvitationEmail(String toEmail, String invitationLink, OffsetDateTime expiresAt) {
        String now = TS.format(OffsetDateTime.now(clock));
        String subject = "[DEV] Clinic Staff Invitation";

        String body = """
                To: %s
                Subject: %s

                You have been invited to join Clinic as staff.

                Invitation link:
                %s

                Expires at: %s

                (DEV NOTE) Use the token from the link query parameter 'token' for local/Postman tests only.
                """.formatted(toEmail, subject, invitationLink, expiresAt);

        try {
            Files.createDirectories(outputDir);

            String safeEmail = toEmail.replaceAll("[^a-zA-Z0-9._-]", "_");
            Path file = outputDir.resolve("invitation_" + safeEmail + "_" + now + ".txt");

            Files.writeString(file, body, StandardCharsets.UTF_8);
            log.info("DEV invitation email generated for {} at {}", toEmail, file.toAbsolutePath());

            if (logFullInvitationLink) {
                log.info("DEV invitation link for {}: {}", toEmail, invitationLink);
            }
        } catch (IOException e) {
            log.warn("Failed to write DEV invitation email for {}", toEmail, e);
        }
    }
}