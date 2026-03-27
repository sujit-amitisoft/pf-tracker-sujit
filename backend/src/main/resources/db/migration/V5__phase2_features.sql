create table rules (
    id uuid primary key,
    user_id uuid not null references users(id) on delete cascade,
    name varchar(140) not null,
    condition_json text not null,
    action_json text not null,
    priority int not null default 100,
    is_active boolean not null default true,
    created_at timestamp not null default now()
);

create table account_members (
    id uuid primary key,
    account_id uuid not null references accounts(id) on delete cascade,
    user_id uuid not null references users(id) on delete cascade,
    role varchar(20) not null,
    invited_by uuid references users(id),
    created_at timestamp not null default now(),
    constraint uq_account_members_account_user unique (account_id, user_id)
);

create index idx_rules_user_priority on rules(user_id, priority, created_at);
create index idx_account_members_account on account_members(account_id);
create index idx_account_members_user on account_members(user_id);