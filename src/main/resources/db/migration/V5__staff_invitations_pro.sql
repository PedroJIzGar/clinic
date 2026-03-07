-- V5__staff_invitations_pro.sql

-- 1) Renombra la tabla vieja (si existe)
ALTER TABLE IF EXISTS staff_invitation
  RENAME TO staff_invitations;

-- 2) Añade columnas (en un ALTER TABLE separado)
ALTER TABLE staff_invitations
  ADD COLUMN IF NOT EXISTS token_hash VARCHAR(128),
  ADD COLUMN IF NOT EXISTS expires_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS last_sent_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS send_count INT NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS accepted_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS revoked_at TIMESTAMPTZ;

-- 3) Índices coherentes con tu modelo actual (email + status)
CREATE INDEX IF NOT EXISTS idx_staff_inv_email_status
  ON staff_invitations (email, status);

CREATE INDEX IF NOT EXISTS idx_staff_inv_status_expires
  ON staff_invitations (status, expires_at);

CREATE INDEX IF NOT EXISTS idx_staff_inv_token_hash
  ON staff_invitations (token_hash);