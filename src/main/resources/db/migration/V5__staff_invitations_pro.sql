-- V5__staff_invitations_pro.sql

ALTER TABLE staff_invitation
   RENAME TO staff_invitations;
  ADD COLUMN IF NOT EXISTS token_hash VARCHAR(128),
  ADD COLUMN IF NOT EXISTS expires_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS last_sent_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS send_count INT NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS accepted_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS revoked_at TIMESTAMPTZ;

-- Acelera búsquedas típicas
CREATE INDEX IF NOT EXISTS idx_staff_inv_email_norm_status
  ON staff_invitations (email_normalized, status);

CREATE INDEX IF NOT EXISTS idx_staff_inv_status_expires
  ON staff_invitations (status, expires_at);

-- Si vas con token:
CREATE INDEX IF NOT EXISTS idx_staff_inv_token_hash
  ON staff_invitations (token_hash);
