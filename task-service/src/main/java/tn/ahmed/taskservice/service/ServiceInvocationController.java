package tn.ahmed.taskservice.service;

import io.dapr.client.DaprClient;
import io.dapr.client.domain.HttpExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import tn.ahmed.taskservice.entities.Task;

import java.util.UUID;

@RestController
@RequestMapping("/tasks-sync")
public class ServiceInvocationController {

    @Autowired
    private DaprClient client;

    private final String STATE_STORE = "statestore";

    @PostMapping
    public Task createTaskWithServiceInvocation(@RequestBody Task task) throws Exception {
        task.setId(UUID.randomUUID().toString());

        // Save task to state store
        client.saveState(STATE_STORE, task.getId(), task).block();

        // Service Invocation: Call notification-service directly
        try {
            var response = client.invokeMethod(
                "notification-service",
                "/notify",
                task,
                HttpExtension.POST,
                String.class
            ).block();
            System.out.println("Service Invocation response: " + response);
        } catch (Exception e) {
            System.err.println("Service invocation failed: " + e.getMessage());
        }

        return task;
    }
}
