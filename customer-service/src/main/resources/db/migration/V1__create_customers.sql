CREATE TABLE customers (
    id                  UUID         NOT NULL,
    email               VARCHAR(254) NOT NULL,
    full_name           TEXT         NOT NULL,
    tax_number          VARCHAR(9)   NOT NULL,
    birth_date          DATE         NOT NULL,
    phone               TEXT         NOT NULL,
    license_number      VARCHAR(20)  NOT NULL,
    license_category    VARCHAR(3)   NOT NULL,
    license_country     VARCHAR(2)   NOT NULL,
    license_issue_date  DATE         NOT NULL,
    license_expiry_date DATE         NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,
    CONSTRAINT customers_pkey PRIMARY KEY (id),
    CONSTRAINT customers_tax_number_unique UNIQUE (tax_number)
);
