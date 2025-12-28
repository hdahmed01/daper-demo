dapr run `
   --app-id notification-service `
   --app-port 8082 `
   --dapr-http-port 3501 `
   --config "C:/Users/PC/Desktop/web/daper/TaskManagement/components/tracing.yaml" `
   --resources-path "C:/Users/PC/Desktop/web/daper/TaskManagement/components" `
   -- java -jar target/notification-service-0.0.1-SNAPSHOT.jar






dapr run `
   --app-id task-service `
   --app-port 8081 `
   --dapr-http-port 3500 `
   --config "C:/Users/PC/Desktop/web/daper/TaskManagement/components/tracing.yaml" `
   --resources-path "C:/Users/PC/Desktop/web/daper/TaskManagement/components" `
   -- java -jar task-service/target/task-service-0.0.1-SNAPSHOT.jar


dapr run `
   --app-id analytics-service `
   --app-port 8083 `
   --dapr-http-port 3502 `
   --config "C:/Users/PC/Desktop/web/daper/TaskManagement/components/tracing.yaml" `
   --resources-path "C:/Users/PC/Desktop/web/daper/TaskManagement/components" `
   -- java -jar analytics-service/target/analytics-service-0.0.1-SNAPSHOT.jar


dapr run `
   --app-id email-service `
   --app-port 8084 `
   --dapr-http-port 3503 `
   --config "C:/Users/PC/Desktop/web/daper/TaskManagement/components/tracing.yaml" `
   --resources-path "C:/Users/PC/Desktop/web/daper/TaskManagement/components" `
   -- java -jar email-service/target/email-service-0.0.1-SNAPSHOT.jar



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






docker run -d -p 1025:1025 -p 8025:8025 --name mailhog mailhog/mailhog
 