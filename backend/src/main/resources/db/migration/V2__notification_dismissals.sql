create table if not exists notification_dismissals (
    id uuid primary key,
    user_id uuid not null references users(id) on delete cascade,
    notification_id uuid not null,
    dismissed_at timestamp not null default now(),
    constraint uq_notification_dismissals_user_notification unique (user_id, notification_id)
);

create index if not exists idx_notification_dismissals_user on notification_dismissals(user_id);
