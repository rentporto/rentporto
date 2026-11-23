#!/bin/bash
# Checks that every container of every Deployment and StatefulSet has readiness and liveness probes and CPU and
# memory limits, that every init container has limits, and that every HorizontalPodAutoscaler scales a Deployment
# whose containers request CPU, starting from the replicas of the Deployment. Needs kubectl and yq.
set -euo pipefail
cd "$(dirname "$0")/../.."

manifests=$(kubectl kustomize --load-restrictor LoadRestrictionsNone infra/k8s/overlays/k3d)

workloads=$(echo "$manifests" | yq ea '[select(.kind == "Deployment" or .kind == "StatefulSet")] | length')
# shellcheck disable=SC2016
missing=$(echo "$manifests" | yq e '
    select(.kind == "Deployment" or .kind == "StatefulSet")
    | .metadata.name as $workload
    | ((.spec.template.spec.containers[]
          | select(.readinessProbe == null or .livenessProbe == null
              or .resources.limits.cpu == null or .resources.limits.memory == null)
          | $workload + "/" + .name),
        (.spec.template.spec.initContainers[]?
          | select(.resources.limits.cpu == null or .resources.limits.memory == null)
          | $workload + "/" + .name + " (init)"))' | grep -v '^---$' || true)

if [[ -n "$missing" ]]; then
    echo "FAIL: containers without probes or limits:"
    echo "$missing"
    exit 1
fi
echo "Kubernetes: $workloads Deployments and StatefulSets, every container with probes and CPU and memory limits"

autoscalers=0
for autoscaler in $(echo "$manifests" | yq e 'select(.kind == "HorizontalPodAutoscaler") | .metadata.name'); do
    [[ "$autoscaler" == "---" ]] && continue
    target=$(echo "$manifests" | yq e "select(.kind == \"HorizontalPodAutoscaler\" and .metadata.name == \"$autoscaler\")
        | .spec.scaleTargetRef.name")
    minimum=$(echo "$manifests" | yq e "select(.kind == \"HorizontalPodAutoscaler\" and .metadata.name == \"$autoscaler\")
        | .spec.minReplicas")
    deployment=$(echo "$manifests" | yq e "select(.kind == \"Deployment\" and .metadata.name == \"$target\")")
    [[ -n "$deployment" ]] || { echo "FAIL: the autoscaler $autoscaler scales $target, which is not a Deployment"; exit 1; }
    without_cpu=$(echo "$deployment" | yq e '[.spec.template.spec.containers[] | select(.resources.requests.cpu == null)]
        | length')
    [[ "$without_cpu" == "0" ]] || { echo "FAIL: $target has containers without a CPU request to scale by"; exit 1; }
    replicas=$(echo "$deployment" | yq e '.spec.replicas')
    [[ "$replicas" == "$minimum" ]] \
        || { echo "FAIL: $target starts with $replicas replicas, but its autoscaler keeps at least $minimum"; exit 1; }
    autoscalers=$((autoscalers + 1))
done
echo "Kubernetes: $autoscalers autoscalers, each on a Deployment that requests CPU and starts from its minimum"
