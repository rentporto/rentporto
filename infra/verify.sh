#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."

services=(auth customer fleet booking rental notification)

for service in "${services[@]}"; do
    password_variable="$(echo "$service" | tr '[:lower:]' '[:upper:]')_DB_PASSWORD"
    password="${!password_variable:-$service}"
    for database in "${services[@]}"; do
        if docker compose exec -T -e PGPASSWORD="$password" postgres \
            psql -h localhost -U "$service" -d "$database" -tAc 'SELECT 1' >/dev/null 2>&1; then
            [[ "$database" == "$service" ]] || { echo "FAIL: user $service can connect to database $database"; exit 1; }
        else
            [[ "$database" != "$service" ]] || { echo "FAIL: user $service cannot connect to database $service"; exit 1; }
        fi
    done
done
echo "PostgreSQL: ${#services[@]} databases, each user only connects to its own"

curl -fsS -u rentporto:rentporto http://localhost:15672/api/overview >/dev/null \
    || { echo "FAIL: cannot log in to RabbitMQ as rentporto"; exit 1; }

expected_queues=$(python3 -c "import json; print(len(json.load(open('infra/rabbitmq/definitions.json'))['queues']))")
expected_bindings=$(python3 -c "import json; print(len(json.load(open('infra/rabbitmq/definitions.json'))['bindings']))")
queues=$(curl -fsS -u rentporto:rentporto http://localhost:15672/api/queues/%2F \
    | python3 -c "import json, sys; print(len(json.load(sys.stdin)))")
bindings=$(curl -fsS -u rentporto:rentporto http://localhost:15672/api/exchanges/%2F/rentporto.events/bindings/source \
    | python3 -c "import json, sys; print(len(json.load(sys.stdin)))")
[[ "$queues" == "$expected_queues" ]] || { echo "FAIL: $queues queues, expected $expected_queues"; exit 1; }
[[ "$bindings" == "$expected_bindings" ]] || { echo "FAIL: $bindings bindings, expected $expected_bindings"; exit 1; }
echo "RabbitMQ: $queues queues and $bindings bindings loaded from definitions.json"
