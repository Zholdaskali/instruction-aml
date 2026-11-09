CREATE TABLE t_app_users (
    id UUID PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    region VARCHAR(100) NOT NULL,
    CONSTRAINT chk_user_role
        CHECK (role IN ('SUPERVISOR', 'INVESTIGATOR'))
);

CREATE TABLE t_case_files (
    id UUID PRIMARY KEY,
    case_number VARCHAR(100) NOT NULL UNIQUE,
    region VARCHAR(100) NOT NULL
);

CREATE TABLE t_case_participants (
    id UUID PRIMARY KEY,
    case_id UUID NOT NULL REFERENCES t_case_files(id),
    user_id UUID NOT NULL REFERENCES t_app_users(id),
    CONSTRAINT uq_case_participant UNIQUE (case_id, user_id)
);

CREATE TABLE t_instructions (
    id UUID PRIMARY KEY,
    case_id UUID NOT NULL REFERENCES t_case_files(id),
    sender_id UUID NOT NULL REFERENCES t_app_users(id),
    recipient_id UUID NOT NULL REFERENCES t_app_users(id),
    text VARCHAR(2000) NOT NULL,
    sent_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE t_notifications (
    id UUID PRIMARY KEY,
    instruction_id UUID NOT NULL UNIQUE REFERENCES t_instructions(id),
    recipient_id UUID NOT NULL REFERENCES t_app_users(id),
    created_at TIMESTAMPTZ NOT NULL,
    read_at TIMESTAMPTZ
);

CREATE INDEX idx_case_region
    ON t_case_files(region);

CREATE INDEX idx_notifications_recipient
    ON t_notifications(recipient_id, created_at DESC);

CREATE INDEX idx_notifications_unread
    ON t_notifications(recipient_id)
    WHERE read_at IS NULL;
