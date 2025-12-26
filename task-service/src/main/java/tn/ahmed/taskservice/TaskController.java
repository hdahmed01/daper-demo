package tn.ahmed.taskservice;


import io.dapr.client.DaprClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    @Autowired
    private DaprClient client;

    private final String STATE_STORE = "statestore";
    private final String PUBSUB_NAME = "pubsub";

    @PostMapping
    public Task createTask(@RequestBody Task task) throws Exception {
        task.setId(UUID.randomUUID().toString());

        // Save task to state store
        client.saveState(STATE_STORE, task.getId(), task).block();

        // Publish event to pub/sub
        client.publishEvent(PUBSUB_NAME, "task-created", task).block();

        return task;
    }

    @GetMapping("/{id}")
    public Task getTask(@PathVariable String id) throws Exception {
        var state = client.getState(STATE_STORE, id, Task.class).block();
        if (state == null || state.getValue() == null) {
            throw new RuntimeException("Task not found");
        }
        return state.getValue();
    }
}
