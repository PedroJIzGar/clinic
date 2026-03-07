-- V6__add_version_to_staff_invitations.sql
ALTER TABLE staff_invitations
  ADD COLUMN IF NOT EXISTS version BIGINT;

UPDATE staff_invitations SET version = 0 WHERE version IS NULL;

ALTER TABLE staff_invitations
  ALTER COLUMN version SET NOT NULL;