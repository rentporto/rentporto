ALTER TABLE rental_agreements
    ADD COLUMN settlement_captured_cents    BIGINT,
    ADD COLUMN settlement_released_cents    BIGINT,
    ADD COLUMN settlement_outstanding_cents BIGINT,
    ADD COLUMN closed_at                    TIMESTAMPTZ;

CREATE INDEX rental_agreements_pickup_at_idx ON rental_agreements (pickup_at);
