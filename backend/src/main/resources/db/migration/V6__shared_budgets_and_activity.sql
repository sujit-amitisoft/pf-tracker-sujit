ALTER TABLE budgets
    ADD COLUMN IF NOT EXISTS account_id UUID,
    ADD COLUMN IF NOT EXISTS created_by UUID,
    ADD COLUMN IF NOT EXISTS updated_by UUID;

CREATE INDEX IF NOT EXISTS idx_budgets_account_id ON budgets(account_id);

CREATE TABLE IF NOT EXISTS account_activity (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    actor_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    activity_type VARCHAR(64) NOT NULL,
    subject_type VARCHAR(64) NOT NULL,
    subject_id UUID,
    summary TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_account_activity_account_created ON account_activity(account_id, created_at DESC);