ALTER TABLE rental_agreements
    ADD COLUMN returned_at               TIMESTAMPTZ,
    ADD COLUMN return_odometer_km        BIGINT,
    ADD COLUMN return_fuel_level_eighths INTEGER,
    ADD COLUMN return_litres_to_fill     NUMERIC(5, 2),
    ADD COLUMN return_notes              TEXT;

CREATE TABLE rental_agreement_damages (
    rental_id    UUID    NOT NULL,
    damage_order INTEGER NOT NULL,
    location     TEXT    NOT NULL,
    description  TEXT    NOT NULL,
    amount_cents BIGINT  NOT NULL,
    CONSTRAINT rental_agreement_damages_pkey PRIMARY KEY (rental_id, damage_order),
    CONSTRAINT rental_agreement_damages_agreement_fkey FOREIGN KEY (rental_id) REFERENCES rental_agreements (id)
);

CREATE TABLE rental_agreement_charges (
    rental_id    UUID        NOT NULL,
    charge_order INTEGER     NOT NULL,
    type         VARCHAR(20) NOT NULL,
    description  TEXT        NOT NULL,
    amount_cents BIGINT      NOT NULL,
    CONSTRAINT rental_agreement_charges_pkey PRIMARY KEY (rental_id, charge_order),
    CONSTRAINT rental_agreement_charges_agreement_fkey FOREIGN KEY (rental_id) REFERENCES rental_agreements (id)
);
