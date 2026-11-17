CREATE TABLE accounts (
    id              UUID         PRIMARY KEY,
    email           VARCHAR(254) NOT NULL UNIQUE,
    password_hash   VARCHAR(100) NOT NULL,
    idempotency_key VARCHAR(255),
    created_at      TIMESTAMPTZ  NOT NULL
);

CREATE TABLE account_roles (
    account_id UUID        NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
    role       VARCHAR(20) NOT NULL,
    PRIMARY KEY (account_id, role)
);
