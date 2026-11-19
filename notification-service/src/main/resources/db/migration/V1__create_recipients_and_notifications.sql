CREATE TABLE recipients (
    id         UUID         NOT NULL,
    full_name  TEXT         NOT NULL,
    email      VARCHAR(254) NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT recipients_pkey PRIMARY KEY (id)
);

CREATE TABLE notifications (
    id           UUID         NOT NULL,
    event_id     UUID         NOT NULL,
    customer_id  UUID         NOT NULL,
    type         VARCHAR(50)  NOT NULL,
    subject      TEXT         NOT NULL,
    email        VARCHAR(254) NOT NULL,
    status       VARCHAR(10)  NOT NULL,
    attempts     INTEGER      NOT NULL,
    last_error   TEXT,
    attempted_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT notifications_pkey PRIMARY KEY (id),
    CONSTRAINT notifications_event_unique UNIQUE (event_id)
);

CREATE INDEX notifications_customer_idx ON notifications (customer_id, attempted_at DESC);
