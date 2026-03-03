create table if not exists app_user (
  id              bigserial primary key,
  firebase_uid    varchar(128) not null unique,
  email           varchar(255) not null,
  role            varchar(32) not null,
  enabled         boolean not null default true,
  created_at      timestamptz not null default now()
);

create table if not exists patient_profile (
  user_id     bigint primary key references app_user(id) on delete cascade,
  full_name   varchar(200) not null,
  phone       varchar(50),
  created_at  timestamptz not null default now()
);

create table if not exists dentist_profile (
  user_id     bigint primary key references app_user(id) on delete cascade,
  full_name   varchar(200) not null,
  active      boolean not null default true,
  created_at  timestamptz not null default now()
);

create table if not exists treatment (
  id               bigserial primary key,
  name             varchar(200) not null,
  duration_minutes int not null check (duration_minutes > 0),
  active           boolean not null default true
);

create table if not exists appointment (
  id            bigserial primary key,
  dentist_id    bigint not null references dentist_profile(user_id),
  patient_id    bigint not null references patient_profile(user_id),
  treatment_id  bigint not null references treatment(id),
  start_at      timestamptz not null,
  end_at        timestamptz not null,
  status        varchar(32) not null,
  notes         text,
  created_at    timestamptz not null default now(),
  check (end_at > start_at)
);

create index if not exists idx_appointment_dentist_time on appointment(dentist_id, start_at, end_at);
create index if not exists idx_appointment_patient_time on appointment(patient_id, start_at, end_at);