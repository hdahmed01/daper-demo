#!/bin/bash

echo "🚀 Setting up Kubernetes deployment files..."

# Create k8s directory
mkdir -p k8s

# ============================================
# 1. KIND CONFIG
# ============================================
cat > k8s/kind-config.yaml << 'EOF'
apiVersion: kind.x-k8s.io/v1alpha4
kind: Cluster
name: taskmanagement-cluster
nodes:
  - role: control-plane
    kubeadmConfigPatches:
      - |
        kind: InitConfiguration
        nodeRegistration:
          kubeletExtraArgs:
            node-labels: "ingress-ready=true"
    extraPortMappings:
      - containerPort: 30081
        hostPort: 8081
        protocol: TCP
      - containerPort: 30082
        hostPort: 8082
        protocol: TCP
      - containerPort: 30083
        hostPort: 8083
        protocol: TCP
      - containerPort: 30084
        hostPort: 8084
        protocol: TCP
      - containerPort: 30085
        hostPort: 8085
        protocol: TCP
      - containerPort: 30090
        hostPort: 8090
        protocol: TCP
      - containerPort: 30017
        hostPort: 27017
        protocol: TCP
      - containerPort: 30333
        hostPort: 6333
        protocol: TCP
      - containerPort: 30686
        hostPort: 16686
        protocol: TCP
      - containerPort: 31025
        hostPort: 1025
        protocol: TCP
      - containerPort: 30025
        hostPort: 8025
        protocol: TCP
EOF

# ============================================
# 2. INFRASTRUCTURE
# ============================================
cat > k8s/infrastructure.yaml << 'EOF'
# REDIS
apiVersion: apps/v1
kind: Deployment
metadata:
  name: redis
spec:
  replicas: 1
  selector:
    matchLabels:
      app: redis
  template:
    metadata:
      labels:
        app: redis
    spec:
      containers:
        - name: redis
          image: redis:7-alpine
          ports:
            - containerPort: 6379
---
apiVersion: v1
kind: Service
metadata:
  name: redis
spec:
  type: NodePort
  selector:
    app: redis
  ports:
    - port: 6379
      targetPort: 6379
      nodePort: 30333

---
# MONGODB
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mongodb
spec:
  replicas: 1
  selector:
    matchLabels:
      app: mongodb
  template:
    metadata:
      labels:
        app: mongodb
    spec:
      containers:
        - name: mongodb
          image: mongo:7-jammy
          ports:
            - containerPort: 27017
          env:
            - name: MONGO_INITDB_ROOT_USERNAME
              value: "admin"
            - name: MONGO_INITDB_ROOT_PASSWORD
              value: "password"
---
apiVersion: v1
kind: Service
metadata:
  name: mongodb
spec:
  type: NodePort
  selector:
    app: mongodb
  ports:
    - port: 27017
      targetPort: 27017
      nodePort: 30017

---
# KEYCLOAK
apiVersion: apps/v1
kind: Deployment
metadata:
  name: keycloak
spec:
  replicas: 1
  selector:
    matchLabels:
      app: keycloak
  template:
    metadata:
      labels:
        app: keycloak
    spec:
      containers:
        - name: keycloak
          image: quay.io/keycloak/keycloak:23.0.0
          args:
            - "start-dev"
          ports:
            - containerPort: 8080
          env:
            - name: KEYCLOAK_ADMIN
              value: "admin"
            - name: KEYCLOAK_ADMIN_PASSWORD
              value: "admin"
---
apiVersion: v1
kind: Service
metadata:
  name: keycloak
spec:
  type: NodePort
  selector:
    app: keycloak
  ports:
    - port: 8080
      targetPort: 8080
      nodePort: 30090

---
# JAEGER
apiVersion: apps/v1
kind: Deployment
metadata:
  name: jaeger
spec:
  replicas: 1
  selector:
    matchLabels:
      app: jaeger
  template:
    metadata:
      labels:
        app: jaeger
    spec:
      containers:
        - name: jaeger
          image: jaegertracing/all-in-one:latest
          ports:
            - containerPort: 16686
            - containerPort: 9411
            - containerPort: 6831
          env:
            - name: COLLECTOR_ZIPKIN_HOST_PORT
              value: ":9411"
---
apiVersion: v1
kind: Service
metadata:
  name: jaeger
spec:
  type: NodePort
  selector:
    app: jaeger
  ports:
    - name: ui
      port: 16686
      targetPort: 16686
      nodePort: 30686
    - name: zipkin
      port: 9411
      targetPort: 9411
    - name: udp
      port: 6831
      targetPort: 6831
      protocol: UDP

---
# MAILHOG
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mailhog
spec:
  replicas: 1
  selector:
    matchLabels:
      app: mailhog
  template:
    metadata:
      labels:
        app: mailhog
    spec:
      containers:
        - name: mailhog
          image: mailhog/mailhog:latest
          ports:
            - containerPort: 1025
            - containerPort: 8025
---
apiVersion: v1
kind: Service
metadata:
  name: mailhog
spec:
  type: NodePort
  selector:
    app: mailhog
  ports:
    - name: smtp
      port: 1025
      targetPort: 1025
      nodePort: 31025
    - name: web
      port: 8025
      targetPort: 8025
      nodePort: 30025
EOF

# ============================================
# 3. DAPR COMPONENTS
# ============================================
cat > k8s/dapr-components.yaml << 'EOF'
# STATE STORE
apiVersion: dapr.io/v1alpha1
kind: Component
metadata:
  name: statestore
spec:
  type: state.redis
  version: v1
  metadata:
    - name: redisHost
      value: redis:6379
    - name: redisPassword
      value: ""

---
# PUB/SUB
apiVersion: dapr.io/v1alpha1
kind: Component
metadata:
  name: pubsub
spec:
  type: pubsub.redis
  version: v1
  metadata:
    - name: redisHost
      value: redis:6379
    - name: redisPassword
      value: ""

---
# TRACING CONFIG
apiVersion: dapr.io/v1alpha1
kind: Configuration
metadata:
  name: tracing
spec:
  tracing:
    samplingRate: "1"
    zipkin:
      endpointAddress: "http://jaeger:9411/api/v2/spans"

---
# EMAIL BINDING
apiVersion: dapr.io/v1alpha1
kind: Component
metadata:
  name: email-binding
spec:
  type: bindings.smtp
  version: v1
  metadata:
    - name: host
      value: "mailhog"
    - name: port
      value: "1025"
    - name: user
      value: ""
    - name: password
      value: ""
    - name: skipTLSVerify
      value: "true"
    - name: emailFrom
      value: "taskmanagement@example.com"
    - name: emailTo
      value: "user@example.com"
    - name: subject
      value: "Task Notification"
  scopes:
    - email-service

---
# CRON BINDING
apiVersion: dapr.io/v1alpha1
kind: Component
metadata:
  name: daily-reset-cron
spec:
  type: bindings.cron
  version: v1
  metadata:
    - name: schedule
      value: "@every 2m"
    - name: direction
      value: "input"
  scopes:
    - analytics-service

---
# SUBSCRIPTIONS
apiVersion: dapr.io/v2alpha1
kind: Subscription
metadata:
  name: analytics-task-created
spec:
  pubsubname: pubsub
  topic: task-created
  routes:
    default: /task-created
  scopes:
    - analytics-service

---
apiVersion: dapr.io/v2alpha1
kind: Subscription
metadata:
  name: analytics-status-changed
spec:
  pubsubname: pubsub
  topic: task-status-changed
  routes:
    default: /task-status-changed
  scopes:
    - analytics-service

---
apiVersion: dapr.io/v2alpha1
kind: Subscription
metadata:
  name: analytics-assigned
spec:
  pubsubname: pubsub
  topic: task-assigned
  routes:
    default: /task-assigned
  scopes:
    - analytics-service

---
apiVersion: dapr.io/v2alpha1
kind: Subscription
metadata:
  name: email-task-created
spec:
  pubsubname: pubsub
  topic: task-created
  routes:
    default: /task-created
  scopes:
    - email-service

---
apiVersion: dapr.io/v2alpha1
kind: Subscription
metadata:
  name: email-assignment
spec:
  pubsubname: pubsub
  topic: task-assigned
  routes:
    default: /task-assigned
  scopes:
    - email-service

---
apiVersion: dapr.io/v2alpha1
kind: Subscription
metadata:
  name: email-status
spec:
  pubsubname: pubsub
  topic: task-status-changed
  routes:
    default: /task-status-changed
  scopes:
    - email-service

---
apiVersion: dapr.io/v2alpha1
kind: Subscription
metadata:
  name: email-priority
spec:
  pubsubname: pubsub
  topic: task-priority-changed
  routes:
    default: /task-priority-changed
  scopes:
    - email-service
EOF

# ============================================
# 4. MICROSERVICES
# ============================================
cat > k8s/microservices.yaml << 'EOF'
# TASK SERVICE
apiVersion: apps/v1
kind: Deployment
metadata:
  name: task-service
spec:
  replicas: 1
  selector:
    matchLabels:
      app: task-service
  template:
    metadata:
      labels:
        app: task-service
      annotations:
        dapr.io/enabled: "true"
        dapr.io/app-id: "task-service"
        dapr.io/app-port: "8081"
        dapr.io/config: "tracing"
    spec:
      containers:
        - name: task-service
          image: task-service:latest
          imagePullPolicy: Never
          ports:
            - containerPort: 8081
          env:
            - name: SPRING_MONGODB_HOST
              value: "mongodb"
            - name: SPRING_MONGODB_PORT
              value: "27017"
            - name: SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI
              value: "http://keycloak:8080/realms/taskmanagement"
---
apiVersion: v1
kind: Service
metadata:
  name: task-service
spec:
  type: NodePort
  selector:
    app: task-service
  ports:
    - port: 8081
      targetPort: 8081
      nodePort: 30081

---
# NOTIFICATION SERVICE
apiVersion: apps/v1
kind: Deployment
metadata:
  name: notification-service
spec:
  replicas: 1
  selector:
    matchLabels:
      app: notification-service
  template:
    metadata:
      labels:
        app: notification-service
      annotations:
        dapr.io/enabled: "true"
        dapr.io/app-id: "notification-service"
        dapr.io/app-port: "8082"
        dapr.io/config: "tracing"
    spec:
      containers:
        - name: notification-service
          image: notification-service:latest
          imagePullPolicy: Never
          ports:
            - containerPort: 8082
---
apiVersion: v1
kind: Service
metadata:
  name: notification-service
spec:
  type: NodePort
  selector:
    app: notification-service
  ports:
    - port: 8082
      targetPort: 8082
      nodePort: 30082

---
# ANALYTICS SERVICE
apiVersion: apps/v1
kind: Deployment
metadata:
  name: analytics-service
spec:
  replicas: 1
  selector:
    matchLabels:
      app: analytics-service
  template:
    metadata:
      labels:
        app: analytics-service
      annotations:
        dapr.io/enabled: "true"
        dapr.io/app-id: "analytics-service"
        dapr.io/app-port: "8083"
        dapr.io/config: "tracing"
    spec:
      containers:
        - name: analytics-service
          image: analytics-service:latest
          imagePullPolicy: Never
          ports:
            - containerPort: 8083
---
apiVersion: v1
kind: Service
metadata:
  name: analytics-service
spec:
  type: NodePort
  selector:
    app: analytics-service
  ports:
    - port: 8083
      targetPort: 8083
      nodePort: 30083

---
# EMAIL SERVICE
apiVersion: apps/v1
kind: Deployment
metadata:
  name: email-service
spec:
  replicas: 1
  selector:
    matchLabels:
      app: email-service
  template:
    metadata:
      labels:
        app: email-service
      annotations:
        dapr.io/enabled: "true"
        dapr.io/app-id: "email-service"
        dapr.io/app-port: "8084"
        dapr.io/config: "tracing"
    spec:
      containers:
        - name: email-service
          image: email-service:latest
          imagePullPolicy: Never
          ports:
            - containerPort: 8084
          env:
            - name: EMAIL_MODE
              value: "smtp"
---
apiVersion: v1
kind: Service
metadata:
  name: email-service
spec:
  type: NodePort
  selector:
    app: email-service
  ports:
    - port: 8084
      targetPort: 8084
      nodePort: 30084

---
# USER SERVICE
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
spec:
  replicas: 1
  selector:
    matchLabels:
      app: user-service
  template:
    metadata:
      labels:
        app: user-service
      annotations:
        dapr.io/enabled: "true"
        dapr.io/app-id: "user-service"
        dapr.io/app-port: "8085"
        dapr.io/config: "tracing"
    spec:
      containers:
        - name: user-service
          image: user-service:latest
          imagePullPolicy: Never
          ports:
            - containerPort: 8085
          env:
            - name: SPRING_MONGODB_HOST
              value: "mongodb"
            - name: SPRING_MONGODB_PORT
              value: "27017"
            - name: KEYCLOAK_SERVER_URL
              value: "http://keycloak:8080"
---
apiVersion: v1
kind: Service
metadata:
  name: user-service
spec:
  type: NodePort
  selector:
    app: user-service
  ports:
    - port: 8085
      targetPort: 8085
      nodePort: 30085
EOF

# ============================================
# 5. DOCKERFILES
# ============================================

# Task Service
cat > task-service/Dockerfile << 'EOF'
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF

# Notification Service
cat > notification-service/Dockerfile << 'EOF'
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF

# Analytics Service
cat > analytics-service/Dockerfile << 'EOF'
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF

# Email Service
cat > email-service/Dockerfile << 'EOF'
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF

# User Service
cat > user-service/Dockerfile << 'EOF'
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF

# ============================================
# 6. DEPLOY SCRIPT
# ============================================
cat > deploy.sh << 'EOF'
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
helm upgrade --install dapr dapr/dapr --version=1.12 --namespace dapr-system --create-namespace --wait

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
EOF

chmod +x deploy.sh

# ============================================
# 7. CLEANUP SCRIPT
# ============================================
cat > cleanup.sh << 'EOF'
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
EOF

chmod +x cleanup.sh

echo ""
echo "✅ All files created successfully!"
echo ""
echo "📁 Created files:"
echo "  ✓ k8s/kind-config.yaml"
echo "  ✓ k8s/infrastructure.yaml"
echo "  ✓ k8s/dapr-components.yaml"
echo "  ✓ k8s/microservices.yaml"
echo "  ✓ task-service/Dockerfile"
echo "  ✓ notification-service/Dockerfile"
echo "  ✓ analytics-service/Dockerfile"
echo "  ✓ email-service/Dockerfile"
echo "  ✓ user-service/Dockerfile"
echo "  ✓ deploy.sh"
echo "  ✓ cleanup.sh"
echo ""
echo "🚀 To deploy, run:"
echo "   ./deploy.sh"