#!/bin/bash

echo "🗑️  Cleaning up TaskManagement Kubernetes Deployment..."

echo "  ➜ Deleting microservices..."
kubectl delete -f k8s/microservices.yaml --ignore-not-found=true

echo "  ➜ Deleting Dapr components..."
kubectl delete -f k8s/dapr-components.yaml --ignore-not-found=true

echo "  ➜ Deleting infrastructure..."
kubectl delete -f k8s/infrastructure.yaml --ignore-not-found=true

echo "  ➜ Uninstalling Dapr..."
helm uninstall dapr --namespace dapr-system || true
kubectl delete namespace dapr-system --ignore-not-found=true

echo "  ➜ Deleting Kind cluster..."
kind delete cluster --name taskmanagement-cluster

echo "✅ Cleanup complete!"
