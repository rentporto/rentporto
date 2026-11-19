CREATE TABLE rental_agreements (
    id                         UUID         NOT NULL,
    reservation_id             UUID         NOT NULL,
    customer_id                UUID         NOT NULL,
    pickup_station             VARCHAR(3)   NOT NULL,
    return_station             VARCHAR(3)   NOT NULL,
    category_code              VARCHAR(4)   NOT NULL,
    pickup_at                  TIMESTAMPTZ  NOT NULL,
    return_at                  TIMESTAMPTZ  NOT NULL,
    daily_rate_cents           BIGINT       NOT NULL,
    coverage                   VARCHAR(50)  NOT NULL,
    excess_cents               BIGINT       NOT NULL,
    deposit_cents              BIGINT       NOT NULL,
    fuel_price_per_litre_cents BIGINT       NOT NULL,
    refuelling_fee_cents       BIGINT       NOT NULL,
    total_cents                BIGINT       NOT NULL,
    status                     VARCHAR(20)  NOT NULL,
    discard_reason             VARCHAR(20),
    created_at                 TIMESTAMPTZ  NOT NULL,
    updated_at                 TIMESTAMPTZ  NOT NULL,
    CONSTRAINT rental_agreements_pkey PRIMARY KEY (id),
    CONSTRAINT rental_agreements_reservation_unique UNIQUE (reservation_id)
);

CREATE TABLE rental_agreement_extras (
    rental_id           UUID        NOT NULL,
    extra_order         INTEGER     NOT NULL,
    code                VARCHAR(50) NOT NULL,
    price_per_day_cents BIGINT      NOT NULL,
    cap_cents           BIGINT      NOT NULL,
    CONSTRAINT rental_agreement_extras_pkey PRIMARY KEY (rental_id, extra_order),
    CONSTRAINT rental_agreement_extras_agreement_fkey FOREIGN KEY (rental_id) REFERENCES rental_agreements (id)
);
