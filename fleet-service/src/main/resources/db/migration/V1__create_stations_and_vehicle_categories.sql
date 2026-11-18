CREATE TABLE stations (
    code VARCHAR(3) NOT NULL,
    name TEXT       NOT NULL,
    CONSTRAINT stations_pkey PRIMARY KEY (code)
);

CREATE TABLE vehicle_categories (
    code                  VARCHAR(4)   NOT NULL,
    name                  TEXT         NOT NULL,
    minimum_age           INTEGER      NOT NULL,
    minimum_license_years INTEGER      NOT NULL,
    idempotency_key       VARCHAR(255),
    created_at            TIMESTAMPTZ  NOT NULL,
    updated_at            TIMESTAMPTZ  NOT NULL,
    CONSTRAINT vehicle_categories_pkey PRIMARY KEY (code)
);
