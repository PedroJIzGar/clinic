create extension if not exists btree_gist;

alter table appointment
add constraint no_overlap_per_dentist
exclude using gist (
  dentist_id with =,
  tstzrange(start_at, end_at, '[)') with &&
)
where (status = 'BOOKED');

