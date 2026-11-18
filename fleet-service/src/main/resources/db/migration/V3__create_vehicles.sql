CREATE TABLE vehicles (
    license_plate      VARCHAR(8)   NOT NULL,
    category_code      VARCHAR(4)   NOT NULL,
    station_code       VARCHAR(3)   NOT NULL,
    status             VARCHAR(20)  NOT NULL,
    odometer_km        BIGINT       NOT NULL,
    fuel_level_eighths INTEGER      NOT NULL,
    idempotency_key    VARCHAR(255),
    created_at         TIMESTAMPTZ  NOT NULL,
    updated_at         TIMESTAMPTZ  NOT NULL,
    CONSTRAINT vehicles_pkey PRIMARY KEY (license_plate),
    CONSTRAINT vehicles_category_fkey FOREIGN KEY (category_code) REFERENCES vehicle_categories (code),
    CONSTRAINT vehicles_station_fkey FOREIGN KEY (station_code) REFERENCES stations (code)
);

CREATE INDEX vehicles_station_category_status_idx ON vehicles (station_code, category_code, status);

CREATE TABLE vehicle_status_changes (
    id            BIGINT       GENERATED ALWAYS AS IDENTITY,
    license_plate VARCHAR(8)   NOT NULL,
    from_status   VARCHAR(20),
    to_status     VARCHAR(20)  NOT NULL,
    reason        TEXT,
    changed_by    VARCHAR(255) NOT NULL,
    changed_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT vehicle_status_changes_pkey PRIMARY KEY (id),
    CONSTRAINT vehicle_status_changes_vehicle_fkey FOREIGN KEY (license_plate) REFERENCES vehicles (license_plate)
);

CREATE INDEX vehicle_status_changes_vehicle_idx ON vehicle_status_changes (license_plate);
