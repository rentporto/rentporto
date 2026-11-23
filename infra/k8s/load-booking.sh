#!/bin/bash
# Puts load on the booking-service of the k3d cluster and follows its HorizontalPodAutoscaler (ADR-28). The load runs
# in a busybox pod that calls the booking-service directly, inside the cluster, since the gateway limits each client
# (ADR-21). The script waits until the autoscaler reaches its maximum and then removes the load; with --scale-down,
# it also waits until the autoscaler is back at its minimum, which takes about five minutes.
#
# Usage: ./infra/k8s/load-booking.sh [--scale-down]
set -euo pipefail

CLUSTER=rentporto
NAMESPACE=rentporto
POD=booking-load
LOADED_URL=http://booking-service:8084/v3/api-docs

kube() {
    kubectl --context "k3d-$CLUSTER" --namespace "$NAMESPACE" "$@"
}

autoscaler() {
    kube get hpa booking-service -o jsonpath="{$1}"
}

report() {
    echo "$(date +%H:%M:%S)  cpu $(autoscaler '.status.currentMetrics[0].resource.current.averageUtilization')%" \
        "of the request (target $(autoscaler '.spec.metrics[0].resource.target.averageUtilization')%)," \
        "$(autoscaler '.status.currentReplicas') replicas"
}

wait_for_replicas() {
    local expected=$1
    for _ in $(seq 1 60); do
        report
        [[ "$(autoscaler '.status.currentReplicas')" == "$expected" ]] && return 0
        sleep 15
    done
    echo "FAIL: the booking-service did not reach $expected replicas"
    return 1
}

scale_down=false
if [[ "${1:-}" == "--scale-down" ]]; then
    scale_down=true
fi
minimum=$(autoscaler '.spec.minReplicas')
maximum=$(autoscaler '.spec.maxReplicas')

kube delete pod "$POD" --ignore-not-found --grace-period=1 >/dev/null
trap 'kube delete pod "$POD" --ignore-not-found --grace-period=1 --wait=false >/dev/null' EXIT
kube apply -f - >/dev/null <<EOF
apiVersion: v1
kind: Pod
metadata:
  name: $POD
  labels:
    app.kubernetes.io/name: $POD
spec:
  restartPolicy: Never
  containers:
    - name: load
      image: busybox:1.37
      command:
        - sh
        - -c
        - for i in \$(seq 1 40); do (while true; do wget -q -O /dev/null $LOADED_URL; done) & done; wait
      resources:
        requests:
          cpu: 100m
          memory: 32Mi
        limits:
          cpu: "1"
          memory: 64Mi
EOF
echo "Load on $LOADED_URL; the booking-service scales from $minimum up to $maximum replicas"
wait_for_replicas "$maximum"
kube delete pod "$POD" --ignore-not-found --grace-period=1 >/dev/null
echo "OK: the booking-service scaled to $maximum replicas; the load is off"

if [[ "$scale_down" == true ]]; then
    wait_for_replicas "$minimum"
    echo "OK: without load, the booking-service is back at $minimum replicas"
fi
