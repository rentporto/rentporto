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

curl -fsS http://localhost:9090/-/ready >/dev/null || { echo "FAIL: Prometheus is not ready"; exit 1; }
rabbitmq_up=""
for _ in $(seq 1 15); do
    rabbitmq_up=$(curl -fsS 'http://localhost:9090/api/v1/query?query=up%7Bjob%3D%22rabbitmq%22%7D' \
        | sed -n 's/.*"value":\[[^,]*,"\([0-9]*\)".*/\1/p')
    [[ "$rabbitmq_up" == "1" ]] && break
    sleep 2
done
[[ "$rabbitmq_up" == "1" ]] || { echo "FAIL: Prometheus does not scrape the queues of RabbitMQ"; exit 1; }
echo "Prometheus: scrapes the queues of RabbitMQ"

grafana="admin:${GRAFANA_PASSWORD:-rentporto}"
curl -fsS -u "$grafana" http://localhost:3000/api/datasources/uid/prometheus/health \
    | grep -q '"status":"OK"' || { echo "FAIL: the Prometheus datasource of Grafana is not working"; exit 1; }
curl -fsS -u "$grafana" http://localhost:3000/api/dashboards/uid/rentporto | grep -q '"uid":"rentporto"' \
    || { echo "FAIL: the RentPorto dashboard is not provisioned in Grafana"; exit 1; }
queries=0
for query in $(python3 infra/grafana/dashboard-queries.py infra/grafana/dashboards/rentporto.json); do
    curl -fsS "http://localhost:9090/api/v1/query?query=$query" 2>/dev/null | grep -q '"status":"success"' \
        || { echo "FAIL: Prometheus rejects a query of the dashboard: $(python3 -c 'import sys, urllib.parse; print(urllib.parse.unquote(sys.argv[1]))' "$query")"; exit 1; }
    queries=$((queries + 1))
done
echo "Grafana: the Prometheus datasource and the RentPorto dashboard are provisioned; Prometheus accepts its $queries queries"
