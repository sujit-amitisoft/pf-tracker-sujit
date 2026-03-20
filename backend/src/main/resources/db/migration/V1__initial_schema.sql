create extension if not exists "pgcrypto";

create table users (
    id uuid primary key,
    email varchar(255) not null unique,
    password_hash text not null,
    display_name varchar(120),
    created_at timestamp not null default now()
);

create table refresh_tokens (
    id uuid primary key,
    user_id uuid not null references users(id) on delete cascade,
    token text not null unique,
    expires_at timestamp not null,
    revoked boolean not null default false,
    created_at timestamp not null default now()
);

create table accounts (
    id uuid primary key,
    user_id uuid not null references users(id) on delete cascade,
    name varchar(100) not null,
    type varchar(30) not null,
    opening_balance numeric(12,2) not null default 0,
    current_balance numeric(12,2) not null default 0,
    institution_name varchar(120),
    created_at timestamp not null default now(),
    last_updated_at timestamp not null default now()
);

create table categories (
    id uuid primary key,
    user_id uuid references users(id) on delete cascade,
    name varchar(100) not null,
    type varchar(20) not null,
    color varchar(20),
    icon varchar(50),
    is_archived boolean not null default false
);

create table budgets (
    id uuid primary key,
    user_id uuid not null references users(id) on delete cascade,
    category_id uuid not null references categories(id),
    month int not null,
    year int not null,
    amount numeric(12,2) not null,
    alert_threshold_percent int default 80,
    constraint uq_budget_user_category_period unique (user_id, category_id, month, year)
);

create table goals (
    id uuid primary key,
    user_id uuid not null references users(id) on delete cascade,
    name varchar(120) not null,
    target_amount numeric(12,2) not null,
    current_amount numeric(12,2) not null default 0,
    target_date date,
    linked_account_id uuid references accounts(id),
    icon varchar(50),
    color varchar(20),
    status varchar(30) not null default 'ACTIVE'
);

create table recurring_transactions (
    id uuid primary key,
    user_id uuid not null references users(id) on delete cascade,
    title varchar(120) not null,
    type varchar(20) not null,
    amount numeric(12,2) not null,
    category_id uuid references categories(id),
    account_id uuid references accounts(id),
    frequency varchar(20) not null,
    start_date date not null,
    end_date date,
    next_run_date date not null,
    auto_create_transaction boolean not null default true,
    paused boolean not null default false
);

create table transactions (
    id uuid primary key,
    user_id uuid not null references users(id) on delete cascade,
    account_id uuid not null references accounts(id),
    category_id uuid references categories(id),
    recurring_transaction_id uuid references recurring_transactions(id),
    type varchar(20) not null,
    amount numeric(12,2) not null,
    transaction_date date not null,
    merchant varchar(200),
    note text,
    payment_method varchar(50),
    tags text,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now()
);

create index idx_accounts_user on accounts(user_id);
create index idx_categories_user on categories(user_id);
create index idx_transactions_user_date on transactions(user_id, transaction_date desc);
create index idx_budgets_user_period on budgets(user_id, year, month);
create index idx_goals_user on goals(user_id);
create index idx_recurring_user_next on recurring_transactions(user_id, next_run_date);

