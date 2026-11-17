#!/bin/bash
set -euo pipefail

for service in auth customer fleet booking rental notification; do
    password_variable="$(echo "$service" | tr '[:lower:]' '[:upper:]')_DB_PASSWORD"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres \
        -v service="$service" -v password="${!password_variable:-$service}" <<'SQL'
CREATE USER :"service" WITH PASSWORD :'password';
CREATE DATABASE :"service" OWNER :"service";
REVOKE ALL ON DATABASE :"service" FROM PUBLIC;
SQL
done
