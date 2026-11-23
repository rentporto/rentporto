#!/bin/bash
# Deploys RentPorto to the k3d cluster "rentporto" (ADR-16, ADR-23): creates the cluster if needed, builds and imports
# the images, creates the Secrets that do not exist yet, applies the k3d overlay, waits until every pod is ready,
# logs in through the Ingress, checks that Prometheus scrapes every application and RabbitMQ, that Grafana has its
# datasource and the RentPorto dashboard, whose queries Prometheus accepts, and that Zipkin has the trace of the login,
# and writes the administrator password for the k3d environment of the Bruno collection.
#
# Usage: ./deploy.sh [--skip-build]
#   --skip-build   import the images already built, without building them again
#   CONFIG_LABEL   branch the Config Server reads from GitHub (default: main)
set -euo pipefail
cd "$(dirname "$0")"

CLUSTER=rentporto
NAMESPACE=rentporto
HOST=rentporto.local
CONFIG_LABEL="${CONFIG_LABEL:-main}"
ADMIN_EMAIL=admin@rentporto.pt
APPS="config-server gateway auth-service customer-service fleet-service booking-service rental-service notification-service"
DATABASES="auth customer fleet booking rental notification"

skip_build=false
if [[ "${1:-}" == "--skip-build" ]]; then
    skip_build=true
fi

kube() {
    kubectl --context "k3d-$CLUSTER" --namespace "$NAMESPACE" "$@"
}

step() {
    echo
    echo "==> $*"
}

random_password() {
    openssl rand -hex 16
}

for tool in docker k3d kubectl openssl curl python3; do
    command -v "$tool" >/dev/null || { echo "Missing $tool"; exit 1; }
done

step "Cluster $CLUSTER"
if k3d cluster list "$CLUSTER" >/dev/null 2>&1; then
    k3d cluster start "$CLUSTER" --wait >/dev/null
    existing_cluster=true
else
    k3d cluster create "$CLUSTER" --agents 1 -p "80:80@loadbalancer" --kubeconfig-switch-context=false --wait
    existing_cluster=false
fi

step "Images"
images=""
for app in $APPS; do
    images="$images rentporto/$app:local"
done
if [[ "$skip_build" == false ]]; then
    ./mvnw -B -q -DskipTests clean package
    for app in $APPS; do
        docker build -q -t "rentporto/$app:local" "$app" >/dev/null
    done
fi
# shellcheck disable=SC2086
k3d image import -c "$CLUSTER" $images

step "Secrets"
kubectl --context "k3d-$CLUSTER" create namespace "$NAMESPACE" --dry-run=client -o yaml \
    | kubectl --context "k3d-$CLUSTER" apply -f - >/dev/null

if ! kube get secret postgres >/dev/null 2>&1; then
    literals="--from-literal=POSTGRES_PASSWORD=$(random_password)"
    for database in $DATABASES; do
        variable="$(echo "$database" | tr '[:lower:]' '[:upper:]')_DB_PASSWORD"
        literals="$literals --from-literal=$variable=$(random_password)"
    done
    # shellcheck disable=SC2086
    kube create secret generic postgres $literals >/dev/null
    echo "created postgres"
fi

if ! kube get secret rabbitmq >/dev/null 2>&1; then
    password=$(random_password)
    users='{"users": [{"name": "rentporto", "password": "'"$password"'", "tags": ["administrator"]}],
"permissions": [{"user": "rentporto", "vhost": "/", "configure": ".*", "write": ".*", "read": ".*"}]}'
    kube create secret generic rabbitmq \
        --from-literal=RABBITMQ_PASSWORD="$password" --from-literal=users.json="$users" >/dev/null
    echo "created rabbitmq"
fi

if ! kube get secret auth-service >/dev/null 2>&1; then
    key_file=$(mktemp)
    openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$key_file" 2>/dev/null
    kube create secret generic auth-service \
        --from-file=AUTH_SIGNING_KEY="$key_file" \
        --from-literal=AUTH_ADMIN_EMAIL="$ADMIN_EMAIL" \
        --from-literal=AUTH_ADMIN_PASSWORD="$(random_password)" >/dev/null
    rm -f "$key_file"
    echo "created auth-service"
fi

if ! kube get secret grafana >/dev/null 2>&1; then
    kube create secret generic grafana --from-literal=GF_SECURITY_ADMIN_PASSWORD="$(random_password)" >/dev/null
    echo "created grafana"
fi

kube create configmap config-server --from-literal=label="$CONFIG_LABEL" --dry-run=client -o yaml \
    | kube apply -f - >/dev/null
echo "the Config Server reads the branch $CONFIG_LABEL"

step "Manifests"
kubectl kustomize --load-restrictor LoadRestrictionsNone infra/k8s/overlays/k3d \
    | kubectl --context "k3d-$CLUSTER" apply -f -

if [[ "$existing_cluster" == true ]]; then
    # shellcheck disable=SC2086
    kube rollout restart deployment $APPS >/dev/null
fi

step "Waiting for the pods"
kube rollout status statefulset/postgres statefulset/rabbitmq --timeout=10m
for app in mailpit prometheus grafana zipkin $APPS; do
    kube rollout status "deployment/$app" --timeout=10m
done
kube get pods

step "Login through http://$HOST"
admin_password=$(kube get secret auth-service -o jsonpath='{.data.AUTH_ADMIN_PASSWORD}' | base64 --decode)
token=""
for _ in $(seq 1 30); do
    token=$(curl -fsS --resolve "$HOST:80:127.0.0.1" -H 'Content-Type: application/json' \
            -d '{"email": "'"$ADMIN_EMAIL"'", "password": "'"$admin_password"'"}' \
            "http://$HOST/api/v1/auth/login" 2>/dev/null \
        | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p') || true
    [[ -n "$token" ]] && break
    sleep 2
done
[[ -n "$token" ]] || { echo "FAIL: could not log in through the Ingress"; exit 1; }

status=""
for _ in $(seq 1 30); do
    status=$(curl -s -o /dev/null -w '%{http_code}' --resolve "$HOST:80:127.0.0.1" \
        -H "Authorization: Bearer $token" \
        "http://$HOST/api/v1/notifications?customerId=00000000-0000-0000-0000-000000000000")
    [[ "$status" == "501" ]] && break
    sleep 2
done
[[ "$status" == "501" ]] || { echo "FAIL: the token was not accepted ($status)"; exit 1; }
echo "OK: the administrator logged in and the gateway and the notification-service accepted the JWT"

status=$(curl -s -o /dev/null -w '%{http_code}' --resolve "$HOST:80:127.0.0.1" "http://$HOST/actuator/prometheus")
[[ "$status" == "404" ]] || { echo "FAIL: the Ingress exposes the metrics of the gateway ($status)"; exit 1; }
echo "OK: the Ingress only routes /api, so the metrics stay inside the cluster"

step "Metrics"
expected=0
# shellcheck disable=SC2086
for replicas in $(kube get deployment $APPS -o jsonpath='{.items[*].spec.replicas}'); do
    expected=$((expected + replicas))
done
prometheus_value() {
    kube exec deployment/prometheus -- wget -qO- "http://localhost:9090/api/v1/query?query=$1" \
        | sed -n 's/.*"value":\[[^,]*,"\([0-9]*\)".*/\1/p'
}
up=0
rabbitmq_up=0
for _ in $(seq 1 30); do
    up=$(prometheus_value 'sum(up%7Bjob%3D%22rentporto%22%7D)')
    rabbitmq_up=$(prometheus_value 'up%7Bjob%3D%22rabbitmq%22%7D')
    [[ "$up" == "$expected" && "$rabbitmq_up" == "1" ]] && break
    sleep 2
done
[[ "$up" == "$expected" ]] || { echo "FAIL: $up of $expected application pods are UP in Prometheus"; exit 1; }
[[ "$rabbitmq_up" == "1" ]] || { echo "FAIL: Prometheus does not scrape the queues of RabbitMQ"; exit 1; }
echo "OK: the $expected application pods and the queues of RabbitMQ are UP in Prometheus"

grafana_password=$(kube get secret grafana -o jsonpath='{.data.GF_SECURITY_ADMIN_PASSWORD}' | base64 --decode)
grafana_auth="Authorization: Basic $(printf 'admin:%s' "$grafana_password" | base64)"
datasource=$(kube exec deployment/grafana -- wget -qO- --header "$grafana_auth" \
    http://localhost:3000/api/datasources/uid/prometheus/health || true)
[[ "$datasource" == *'"status":"OK"'* ]] || { echo "FAIL: the Prometheus datasource of Grafana is not working"; exit 1; }
echo "OK: Grafana has the Prometheus datasource provisioned and working"

dashboard=$(kube exec deployment/grafana -- wget -qO- --header "$grafana_auth" \
    http://localhost:3000/api/dashboards/uid/rentporto || true)
[[ "$dashboard" == *'"uid":"rentporto"'* ]] || { echo "FAIL: the RentPorto dashboard is not provisioned in Grafana"; exit 1; }
queries=0
for query in $(python3 infra/grafana/dashboard-queries.py infra/grafana/dashboards/rentporto.json); do
    answer=$(kube exec deployment/prometheus -- wget -qO- "http://localhost:9090/api/v1/query?query=$query" || true)
    [[ "$answer" == *'"status":"success"'* ]] \
        || { echo "FAIL: Prometheus rejects a query of the dashboard: $(python3 -c 'import sys, urllib.parse; print(urllib.parse.unquote(sys.argv[1]))' "$query")"; exit 1; }
    queries=$((queries + 1))
done
echo "OK: Grafana has the RentPorto dashboard, and Prometheus accepts its $queries queries"

step "Traces"
trace=""
for _ in $(seq 1 30); do
    trace=$(kube exec deployment/zipkin -- wget -qO- \
        'http://localhost:9411/api/v2/traces?serviceName=auth-service&spanName=http%20post%20%2Fauth%2Flogin&limit=1' \
        || true)
    [[ "$trace" == *'"serviceName":"gateway"'* && "$trace" == *'"serviceName":"auth-service"'* ]] && break
    sleep 2
done
[[ "$trace" == *'"serviceName":"gateway"'* && "$trace" == *'"serviceName":"auth-service"'* ]] \
    || { echo "FAIL: Zipkin has no trace of the login through the gateway and the auth-service"; exit 1; }
echo "OK: Zipkin has the trace of the login, through the gateway and the auth-service"

printf 'ADMIN_PASSWORD=%s\n' "$admin_password" > docs/bruno/.env
echo "Bruno: the k3d environment reads the administrator password from docs/bruno/.env"

if ! grep -q "[[:space:]]$HOST" /etc/hosts; then
    echo
    echo "To reach http://$HOST from your machine, add to /etc/hosts:"
    echo "127.0.0.1 $HOST"
    echo "::1 $HOST"
fi

echo
echo "Administrator: $ADMIN_EMAIL; password:"
echo "  kubectl --context k3d-$CLUSTER -n $NAMESPACE get secret auth-service -o jsonpath='{.data.AUTH_ADMIN_PASSWORD}' | base64 --decode"
echo "Grafana: kubectl --context k3d-$CLUSTER -n $NAMESPACE port-forward svc/grafana 3000, then the RentPorto dashboard at http://localhost:3000/d/rentporto as admin; password:"
echo "  kubectl --context k3d-$CLUSTER -n $NAMESPACE get secret grafana -o jsonpath='{.data.GF_SECURITY_ADMIN_PASSWORD}' | base64 --decode"
echo "Zipkin: kubectl --context k3d-$CLUSTER -n $NAMESPACE port-forward svc/zipkin 9411, then http://localhost:9411"
echo "Remove everything: k3d cluster delete $CLUSTER"
