CREATE TABLE pickup_reminders (
    reservation_id UUID        NOT NULL,
    customer_id    UUID        NOT NULL,
    pickup_station VARCHAR(3),
    pickup_at      TIMESTAMPTZ,
    status         VARCHAR(10) NOT NULL,
    CONSTRAINT pickup_reminders_pkey PRIMARY KEY (reservation_id),
    CONSTRAINT pickup_reminders_pickup_known
        CHECK (status = 'CANCELLED' OR (pickup_station IS NOT NULL AND pickup_at IS NOT NULL))
);

CREATE INDEX pickup_reminders_due_idx ON pickup_reminders (status, pickup_at);
