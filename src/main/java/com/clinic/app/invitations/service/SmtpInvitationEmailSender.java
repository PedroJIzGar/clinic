package com.clinic.app.invitations.service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;

@Component
@Profile("!dev")
public class SmtpInvitationEmailSender implements InvitationEmailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpInvitationEmailSender.class);
    private static final DateTimeFormatter TS = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final JavaMailSender mailSender;

    public SmtpInvitationEmailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendStaffInvitationEmail(String toEmail, String invitationLink, OffsetDateTime expiresAt) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Clinic staff invitation");
        message.setText("""
                You have been invited to join Clinic as staff.

                Invitation link:
                %s

                Expires at:
                %s
                """.formatted(invitationLink, TS.format(expiresAt)));

        mailSender.send(message);
        log.info("Staff invitation email sent to {}", toEmail);
    }
}