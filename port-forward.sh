#!/bin/bash

echo "��� Setting up Port Forwarding for Services"
echo "==========================================="
echo ""
echo "This script will forward all services to localhost"
echo "Press Ctrl+C to stop all port forwards"
echo ""

cleanup() {
    echo ""
    echo "��� Stopping all port forwards..."
    jobs -p | xargs -r kill 2>/dev/null
    echo "✅ Cleanup complete"
    exit 0
}

trap cleanup SIGINT SIGTERM EXIT

echo "⏳ Waiting for deployments to be ready..."
kubectl wait --for=condition=available --timeout=300s deployment/task-service 2>/dev/null &
kubectl wait --for=condition=available --timeout=300s deployment/notification-service 2>/dev/null &
kubectl wait --for=condition=available --timeout=300s deployment/analytics-service 2>/dev/null &
kubectl wait --for=condition=available --timeout=300s deployment/email-service 2>/dev/null &
kubectl wait --for=condition=available --timeout=300s deployment/user-service 2>/dev/null &
kubectl wait --for=condition=available --timeout=300s deployment/keycloak 2>/dev/null &
kubectl wait --for=condition=available --timeout=300s deployment/mongodb 2>/dev/null &
kubectl wait --for=condition=available --timeout=300s deployment/redis 2>/dev/null &
kubectl wait --for=condition=available --timeout=300s deployment/jaeger 2>/dev/null &
kubectl wait --for=condition=available --timeout=300s deployment/mailhog 2>/dev/null &
wait

echo ""
echo "✅ All deployments ready!"
echo ""
echo "��� Starting port forwards..."
echo ""

kubectl port-forward service/task-service 8081:8081 > /dev/null 2>&1 &
echo "  ✅ Task Service:         http://localhost:8081"

kubectl port-forward service/notification-service 8082:8082 > /dev/null 2>&1 &
echo "  ✅ Notification Service: http://localhost:8082"

kubectl port-forward service/analytics-service 8083:8083 > /dev/null 2>&1 &
echo "  ✅ Analytics Service:    http://localhost:8083"

kubectl port-forward service/email-service 8084:8084 > /dev/null 2>&1 &
echo "  ✅ Email Service:        http://localhost:8084"

kubectl port-forward service/user-service 8085:8085 > /dev/null 2>&1 &
echo "  ✅ User Service:         http://localhost:8085"

echo ""
echo "Infrastructure:"
echo ""

kubectl port-forward service/keycloak 8090:8080 > /dev/null 2>&1 &
echo "  ✅ Keycloak:            http://localhost:8090 (admin/admin)"

kubectl port-forward service/mongodb 27017:27017 > /dev/null 2>&1 &
echo "  ✅ MongoDB:             localhost:27017 (admin/password)"

kubectl port-forward service/redis 6333:6379 > /dev/null 2>&1 &
echo "  ✅ Redis:               localhost:6333"

kubectl port-forward service/jaeger 16686:16686 > /dev/null 2>&1 &
echo "  ✅ Jaeger UI:           http://localhost:16686"

kubectl port-forward service/mailhog 8025:8025 > /dev/null 2>&1 &
echo "  ✅ MailHog UI:          http://localhost:8025"

kubectl port-forward service/mailhog 1025:1025 > /dev/null 2>&1 &
echo "  ✅ MailHog SMTP:        localhost:1025"

echo ""
echo "═══════════════════════════════════════════════════════════════════"
echo "✅ All services are now accessible!"
echo "═══════════════════════════════════════════════════════════════════"
echo ""
echo "Keep this terminal open. Press Ctrl+C to stop all port forwards."
echo ""

wait
