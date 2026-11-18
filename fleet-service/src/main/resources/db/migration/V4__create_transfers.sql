CREATE TABLE transfers (
    id              UUID         NOT NULL,
    license_plate   VARCHAR(8)   NOT NULL,
    from_station    VARCHAR(3)   NOT NULL,
    to_station      VARCHAR(3)   NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    idempotency_key VARCHAR(255),
    started_at      TIMESTAMPTZ  NOT NULL,
    completed_at    TIMESTAMPTZ,
    CONSTRAINT transfers_pkey PRIMARY KEY (id),
    CONSTRAINT transfers_vehicle_fkey FOREIGN KEY (license_plate) REFERENCES vehicles (license_plate),
    CONSTRAINT transfers_from_station_fkey FOREIGN KEY (from_station) REFERENCES stations (code),
    CONSTRAINT transfers_to_station_fkey FOREIGN KEY (to_station) REFERENCES stations (code),
    CONSTRAINT transfers_idempotency_key_unique UNIQUE (license_plate, idempotency_key)
);

CREATE UNIQUE INDEX transfers_one_open_per_vehicle ON transfers (license_plate) WHERE status = 'IN_TRANSFER';
