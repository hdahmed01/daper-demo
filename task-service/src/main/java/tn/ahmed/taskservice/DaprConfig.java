package tn.ahmed.taskservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.serializer.DaprObjectSerializer;
import io.dapr.utils.TypeRef;
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