package tn.ahmed.notificationservice;

import org.springframework.web.bind.annotation.*;

@RestController
public class ServiceInvocationHandler {

    @PostMapping(path = "/notify")
    public String handleNotification(@RequestBody String taskJson) {
        System.out.println("Service Invocation - Received task: " + taskJson);
        // Handle the task notification
        return "Task notification processed successfully";
    }
}
