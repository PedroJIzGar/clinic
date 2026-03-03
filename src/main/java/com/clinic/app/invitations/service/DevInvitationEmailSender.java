package com.clinic.app.invitations.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevInvitationEmailSender implements InvitationEmailSender {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    @Override
    public void sendStaffInvitationEmail(String toEmail, String invitationLink, OffsetDateTime expiresAt) {
        String now = TS.format(OffsetDateTime.now());
        String subject = "[DEV] Clinic Staff Invitation";
        String body = """
                To: %s
                Subject: %s

                You have been invited to join Clinic as staff.

                Invitation link:
                %s

                Expires at: %s

                (DEV NOTE) Copy the token from the link query param 'token' for Postman tests.
                """.formatted(toEmail, subject, invitationLink, expiresAt);

        // 1) Console/log output (copy friendly)
        System.out.println("\n================= DEV EMAIL (INVITATION) =================");
        System.out.println(body);
        System.out.println("==========================================================\n");

        // 2) Write to file (easier than hunting logs)
        try {
            Path dir = Paths.get("dev-mails");
            Files.createDirectories(dir);

            // sanitize filename
            String safeEmail = toEmail.replaceAll("[^a-zA-Z0-9._-]", "_");
            Path file = dir.resolve("invitation_" + safeEmail + "_" + now + ".txt");

            Files.writeString(file, body, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            // If file write fails, logs already contain the email.
            System.err.println("DEV mail write failed: " + e.getMessage());
        }
    }
}