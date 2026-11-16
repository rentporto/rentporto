CREATE TABLE outbox (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id     UUID         NOT NULL UNIQUE,
    exchange     VARCHAR(255) NOT NULL,
    routing_key  VARCHAR(255) NOT NULL,
    message_type VARCHAR(255) NOT NULL,
    payload      JSONB        NOT NULL,
    headers      JSONB        NOT NULL DEFAULT '{}',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ
);

CREATE INDEX outbox_pending_idx ON outbox (id) WHERE published_at IS NULL;

CREATE TABLE processed_events (
    consumer     VARCHAR(255) NOT NULL,
    event_id     UUID         NOT NULL,
    processed_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (consumer, event_id)
);

CREATE TABLE scheduler_lock (
    name         VARCHAR(64)  NOT NULL PRIMARY KEY,
    locked_until TIMESTAMPTZ  NOT NULL,
    locked_at    TIMESTAMPTZ  NOT NULL,
    locked_by    VARCHAR(255) NOT NULL
);
