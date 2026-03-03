CREATE TABLE staff_invitation (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(255) NOT NULL,
  role VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_by_user_id BIGINT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  accepted_at TIMESTAMPTZ NULL
);

-- Solo una invitación PENDING por email (la clave)
CREATE UNIQUE INDEX uq_staff_invitation_email_pending
ON staff_invitation (email)
WHERE status = 'PENDING';

-- Si quieres forzar staff roles vía DB (opcional):
-- CHECK (role IN ('DENTIST','RECEPTIONIST','ADMIN'))

ALTER TABLE app_user
  ADD CONSTRAINT uq_app_user_email UNIQUE (email);