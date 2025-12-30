package tn.ahmed.taskservice.config;

import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DaprConfig {

    @Bean
    public DaprClient daprClient() {


        // Build DaprClient with custom serializer
        return new DaprClientBuilder()

                .build();
    }
}