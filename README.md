# TaskManagement - Dapr Learning Project

A learning project demonstrating **Dapr** with Java microservices: task-service and notification-service.

---

## Features

- **State Management:** Save and retrieve data with Dapr, supporting Redis, MongoDB, Cassandra, etc.  
- **Pub/Sub Messaging:** Async communication between services using topics.  
- **Service Invocation:** Synchronous calls between services without hard-coded URLs.  
- **Distributed Tracing:** Automatic observability with Jaeger.  

---

## Project Structure

```
TaskManagement/
├── components/              # Dapr configs (state, pubsub, tracing)
├── task-service/           # Microservice 1
└── notification-service/   # Microservice 2
```

---

## Quick Start

### 1. Start Redis
```bash
docker run -d --name redis -p 6333:6379 redis:latest
```

### 2. Start Jaeger
```bash
docker run -d `
  --name jaeger `
  -e COLLECTOR_ZIPKIN_HOST_PORT=:9411 `
  -p 5775:5775/udp `
  -p 6831:6831/udp `
  -p 6832:6832/udp `
  -p 5778:5778 `
  -p 16686:16686 `
  -p 14268:14268 `
  -p 14250:14250 `
  -p 9411:9411 `
  jaegertracing/all-in-one:latest
```

### 3. Run Services with Dapr

**Notification Service:**
```bash
dapr run `
   --app-id notification-service `
   --app-port 8082 `
   --dapr-http-port 3501 `
   --config "path to   tracing.yaml" `
   --resources-path "path to  /TaskManagement/components" `
   -- java -jar target/notification-service-0.0.1-SNAPSHOT.jar
```

**Task Service:**
```bash
dapr run `
   --app-id task-service `
   --app-port 8081 `
   --dapr-http-port 3500 `
   --config "path to  tracing.yaml" `
   --resources-path "path to  /TaskManagement/components" `
   -- java -jar task-service/target/task-service-0.0.1-SNAPSHOT.jar
```

---

### 4. Test Endpoints

- `POST /tasks` → Create task (async pub/sub)  
- `POST /tasks-sync` → Create task (sync service invocation)  
- `GET /tasks/{id}` → Get task  

---

## Key Concepts Learned

- Sidecar architecture  
- State management across multiple stores  
- Pub/Sub event-driven communication  
- Service-to-service invocation  
- Distributed tracing and observability  

---

## Tech Stack

- Java 17  
- Spring Boot  
- Dapr  
- Redis (State & Pub/Sub)  
- Jaeger (Tracing)  

---