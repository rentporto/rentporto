ALTER TABLE rental_agreements
    ADD COLUMN deposit_id                UUID,
    ADD COLUMN license_plate             VARCHAR(8),
    ADD COLUMN picked_up_at              TIMESTAMPTZ,
    ADD COLUMN pickup_odometer_km        BIGINT,
    ADD COLUMN pickup_fuel_level_eighths INTEGER,
    ADD COLUMN pickup_notes              TEXT;

CREATE TABLE rental_agreement_drivers (
    rental_id           UUID        NOT NULL,
    driver_order        INTEGER     NOT NULL,
    full_name           TEXT        NOT NULL,
    license_number      VARCHAR(20) NOT NULL,
    license_category    VARCHAR(3)  NOT NULL,
    license_country     VARCHAR(2)  NOT NULL,
    license_issue_date  DATE        NOT NULL,
    license_expiry_date DATE        NOT NULL,
    CONSTRAINT rental_agreement_drivers_pkey PRIMARY KEY (rental_id, driver_order),
    CONSTRAINT rental_agreement_drivers_agreement_fkey FOREIGN KEY (rental_id) REFERENCES rental_agreements (id)
);
