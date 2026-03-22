alter table users
  add column if not exists avatar_content_type text,
  add column if not exists avatar_bytes bytea,
  add column if not exists avatar_updated_at timestamp;

create index if not exists idx_users_avatar_updated_at on users(avatar_updated_at);
