#!/bin/bash
# Runs RentPorto on this machine for development: the infrastructure in Docker Compose and the services from their jars,
# with the dev profile, in which the Config Server reads the local config/ folder.
#
# Usage:
#   ./dev.sh                     build and start every service in the background, the Config Server first
#   ./dev.sh <service> [args]    build and run one service in this terminal, with the others as they are; Ctrl+C stops
#                                it. The arguments go to the service, e.g.
#                                ./dev.sh fleet-service --logging.level.pt.rentporto=debug
#   ./dev.sh stop                stop the services and the infrastructure, keeping the data
#
# The services started in the background write their logs to <service>/target/<service>.log.
set -euo pipefail
cd "$(dirname "$0")"

SERVICES="auth-service customer-service fleet-service booking-service rental-service notification-service gateway"

port() {
    case "$1" in
        config-server) echo 8888 ;;
        gateway) echo 8080 ;;
        auth-service) echo 8081 ;;
        customer-service) echo 8082 ;;
        fleet-service) echo 8083 ;;
        booking-service) echo 8084 ;;
        rental-service) echo 8085 ;;
        notification-service) echo 8086 ;;
        *) return 1 ;;
    esac
}

step() {
    echo
    echo "==> $*"
}

infrastructure() {
    step "Infrastructure"
    docker compose up -d --wait
}

build() {
    step "Build"
    ./mvnw -q -DskipTests "$@" clean package
}

running() {
    pgrep -f "target/$1-.*\.jar" >/dev/null
}

healthy() {
    curl -sf "http://localhost:$(port "$1")/actuator/health" >/dev/null
}

jar_of() {
    local jar
    for jar in "$1"/target/"$1"-*.jar; do
        # skips the copies that a synced folder (e.g. iCloud) makes, such as "auth-service-0.1.0-SNAPSHOT 2.jar"
        if [[ "$jar" != *" "* ]]; then
            echo "${jar#"$1"/}"
            return
        fi
    done
}

start() {
    local jar
    jar=$(jar_of "$1")
    (cd "$1" && exec nohup java -jar "$jar" < /dev/null > "target/$1.log" 2>&1) &
}

stop() {
    pkill -f "target/$1-.*\.jar" || true
    while running "$1"; do
        sleep 1
    done
}

wait_for() {
    for _ in $(seq 1 180); do
        healthy "$1" && return 0
        running "$1" || return 1
        sleep 1
    done
    return 1
}

run_all() {
    infrastructure
    for service in config-server $SERVICES; do
        stop "$service"
    done
    build

    step "Services"
    start config-server
    if ! wait_for config-server; then
        echo "config-server did not start, see config-server/target/config-server.log"
        exit 1
    fi
    for service in $SERVICES; do
        start "$service"
    done
    local failed=0
    for service in config-server $SERVICES; do
        if wait_for "$service"; then
            printf '  %-21s %s  up\n' "$service" "$(port "$service")"
        else
            printf '  %-21s %s  FAILED, see %s/target/%s.log\n' "$service" "$(port "$service")" "$service" "$service"
            failed=1
        fi
    done

    echo
    echo "API:        http://localhost:8080/api/v1"
    echo "Swagger UI: http://localhost:8080/api/docs"
    echo "Mailpit:    http://localhost:8025"
    echo "Logs:       <service>/target/<service>.log"
    echo "Stop:       ./dev.sh stop"
    exit "$failed"
}

run_one() {
    local service=$1
    shift
    infrastructure
    stop "$service"
    if [[ "$service" == config-server ]] || healthy config-server; then
        build -pl "$service" -am
    else
        build -pl "config-server,$service" -am
        start config-server
        wait_for config-server || { echo "config-server did not start"; exit 1; }
    fi

    step "$service on port $(port "$service"); Ctrl+C stops it"
    local jar
    jar=$(jar_of "$service")
    cd "$service"
    exec java -jar "$jar" "$@"
}

for tool in docker java curl; do
    command -v "$tool" >/dev/null || { echo "Missing $tool"; exit 1; }
done

case "${1:-}" in
    "")
        run_all
        ;;
    stop)
        for service in $SERVICES config-server; do
            stop "$service"
        done
        docker compose stop
        ;;
    *)
        if ! port "$1" >/dev/null; then
            echo "Unknown service: $1"
            echo "Services: config-server $SERVICES"
            exit 1
        fi
        run_one "$@"
        ;;
esac
