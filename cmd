



dapr run --app-id task-service --app-port 8081 --dapr-http-port 3500 --config tracing --resources-path "C:/Users/PC/Desktop/web/daper/TaskManagement/components" -- java -jar task-service/target/task-service-0.0.1-SNAPSHOT.jar


dapr run --app-id notification-service --app-port 8082 --dapr-http-port 3501 --resources-path ./../components ` 
-- java -jar target/notification-service-0.0.1-SNAPSHOT.jar




docker run -d   --name jaeger   -p 6831:6831/udp   -p 6832:6832/udp   -p 16686:16686   jaegertracing/all-in-one