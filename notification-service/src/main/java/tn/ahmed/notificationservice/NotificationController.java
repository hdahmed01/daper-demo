package tn.ahmed.notificationservice;

import io.dapr.client.domain.CloudEvent;
import org.springframework.web.bind.annotation.*;

@RestController
public class NotificationController {

    @PostMapping(path = "/task-created")
    public void onTaskCreated(@RequestBody CloudEvent event) {
        System.out.println("Received new task event: " + event.getData());
    }
}
