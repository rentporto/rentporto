ALTER TABLE vehicles ADD COLUMN status_changed_at TIMESTAMPTZ;
UPDATE vehicles SET status_changed_at = updated_at;
ALTER TABLE vehicles ALTER COLUMN status_changed_at SET NOT NULL;

CREATE TABLE vehicle_assignments (
    rental_id       UUID         NOT NULL,
    license_plate   VARCHAR(8)   NOT NULL,
    station_code    VARCHAR(3)   NOT NULL,
    category_code   VARCHAR(4)   NOT NULL,
    odometer_km     BIGINT       NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    assigned_at     TIMESTAMPTZ  NOT NULL,
    CONSTRAINT vehicle_assignments_pkey PRIMARY KEY (rental_id),
    CONSTRAINT vehicle_assignments_vehicle_fkey FOREIGN KEY (license_plate) REFERENCES vehicles (license_plate),
    CONSTRAINT vehicle_assignments_station_fkey FOREIGN KEY (station_code) REFERENCES stations (code),
    CONSTRAINT vehicle_assignments_category_fkey FOREIGN KEY (category_code) REFERENCES vehicle_categories (code)
);

CREATE INDEX vehicle_assignments_vehicle_idx ON vehicle_assignments (license_plate);
