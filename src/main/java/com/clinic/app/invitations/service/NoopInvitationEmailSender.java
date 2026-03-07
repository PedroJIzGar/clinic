package com.clinic.app.invitations.service;

import java.time.OffsetDateTime;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!dev")
public class NoopInvitationEmailSender implements InvitationEmailSender {

  @Override
  public void sendStaffInvitationEmail(String toEmail, String invitationLink, OffsetDateTime expiresAt) {
    // No-op (en prod aquí meterás SMTP/SendGrid/etc.)
  }
}