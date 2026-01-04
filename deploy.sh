#!/bin/bash

set -e

echo "🚀 Starting TaskManagement Kubernetes Deployment"

echo ""
echo "📦 Step 1: Creating Kind Cluster..."
kind create cluster --config=k8s/kind-config.yaml

echo ""
echo "🔨 Step 2: Building Docker Images..."
echo "  ➜ Building task-service..."
cd task-service && docker build -t task-service:latest . && cd ..

echo "  ➜ Building notification-service..."
cd notification-service && docker build -t notification-service:latest . && cd ..

echo "  ➜ Building analytics-service..."
cd analytics-service && docker build -t analytics-service:latest . && cd ..

echo "  ➜ Building email-service..."
cd email-service && docker build -t email-service:latest . && cd ..

echo "  ➜ Building user-service..."
cd user-service && docker build -t user-service:latest . && cd ..

echo ""
echo "📤 Step 3: Loading Images into Kind..."
kind load docker-image task-service:latest --name taskmanagement-cluster
kind load docker-image notification-service:latest --name taskmanagement-cluster
kind load docker-image analytics-service:latest --name taskmanagement-cluster
kind load docker-image email-service:latest --name taskmanagement-cluster
kind load docker-image user-service:latest --name taskmanagement-cluster

echo ""
echo "🎯 Step 4: Installing Dapr..."
helm repo add dapr https://dapr.github.io/helm-charts/
helm repo update
helm upgrade --install dapr dapr/dapr --version=1.16.5 --namespace dapr-system --create-namespace --wait

echo ""
echo "🏗️  Step 5: Deploying Infrastructure..."
kubectl apply -f k8s/infrastructure.yaml

echo "  ⏳ Waiting for infrastructure..."
sleep 10

echo ""
echo "⚙️  Step 6: Deploying Dapr Components..."
kubectl apply -f k8s/dapr-components.yaml

echo ""
echo "🎪 Step 7: Deploying Microservices..."
kubectl apply -f k8s/microservices.yaml

echo ""
echo "✅ Deployment Complete!"
echo ""
echo "═══════════════════════════════════════════════════════════════════"
echo "📋 SERVICE ENDPOINTS"
echo "═══════════════════════════════════════════════════════════════════"
echo "  Task Service:         http://localhost:8081"
echo "  Notification Service: http://localhost:8082"
echo "  Analytics Service:    http://localhost:8083"
echo "  Email Service:        http://localhost:8084"
echo "  User Service:         http://localhost:8085"
echo ""
echo "═══════════════════════════════════════════════════════════════════"
echo "🔧 INFRASTRUCTURE"
echo "═══════════════════════════════════════════════════════════════════"
echo "  Keycloak:    http://localhost:8090 (admin/admin)"
echo "  MongoDB:     localhost:27017 (admin/password)"
echo "  Redis:       localhost:6333"
echo "  Jaeger UI:   http://localhost:16686"
echo "  MailHog UI:  http://localhost:8025"
echo ""
echo "═══════════════════════════════════════════════════════════════════"
echo "📊 USEFUL COMMANDS"
echo "═══════════════════════════════════════════════════════════════════"
echo "  View pods:     kubectl get pods"
echo "  View services: kubectl get svc"
echo "  View logs:     kubectl logs -f deployment/task-service"
echo "  Delete all:    ./cleanup.sh"
echo "═══════════════════════════════════════════════════════════════════"
