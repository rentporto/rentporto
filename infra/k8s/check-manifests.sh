#!/bin/bash
# Checks that every container of every Deployment and StatefulSet has readiness and liveness probes and CPU and
# memory limits, and that every init container has limits. Needs kubectl and yq.
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
