package com.clinic.app.invitations.service;

import java.time.OffsetDateTime;


public interface InvitationEmailSender {

  void sendStaffInvitationEmail(String toEmail, String invitationLink, OffsetDateTime expiresAt);
}