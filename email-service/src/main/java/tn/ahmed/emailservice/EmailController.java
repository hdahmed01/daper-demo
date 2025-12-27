package tn.ahmed.emailservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.domain.CloudEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Email Controller
 *
 * Endpoints:
 * 1. POST /task-created - Receives events from Dapr Pub/Sub
 * 2. POST /send-email - Manual email sending (testing)
 *
 * Flow:
 * Task Service → Pub/Sub → Dapr Sidecar → POST /task-created
 *                                                ↓
 *                                         EmailSender
 *                                                ↓
 *                                   Mock (console) or SMTP (real email)
 */
@RestController
public class EmailController {

    @Autowired
    private EmailSender emailSender;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * ═══════════════════════════════════════════════════════════════
     * PUB/SUB SUBSCRIBER - Receives task-created events
     * ═══════════════════════════════════════════════════════════════
     *
     * Called automatically by Dapr when task is created
     * Configuration: components/email-subscription.yaml
     */
    @PostMapping("/task-created")
    public void onTaskCreated(@RequestBody CloudEvent event) {
        System.out.println("📧 Email Service: Received task-created event");
        System.out.println("   Event ID: " + event.getId());
        System.out.println("   Source: " + event.getSource());

        try {
            // Extract task data from CloudEvent - getData() returns a Map
            Object rawData = event.getData();
            Map<String, Object> data;

            if (rawData instanceof Map) {
                data = (Map<String, Object>) rawData;
            } else {
                // Fallback: convert to Map using ObjectMapper
                data = objectMapper.convertValue(rawData, Map.class);
            }

            String taskId = data.containsKey("id") ? String.valueOf(data.get("id")) : "unknown";
            String title = data.containsKey("title") ? String.valueOf(data.get("title")) : "Untitled Task";
            String description = data.containsKey("description") ?
                    String.valueOf(data.get("description")) : "No description";

            System.out.println("   Task ID: " + taskId);
            System.out.println("   Title: " + title);

            // Send email notification
            emailSender.sendTaskCreatedEmail(title, description, taskId);

        } catch (Exception e) {
            System.err.println("❌ Error processing email notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * ═══════════════════════════════════════════════════════════════
     * MANUAL EMAIL - For testing purposes
     * ═══════════════════════════════════════════════════════════════
     *
     * Usage:
     * curl -X POST http://localhost:8084/send-email \
     *   -H "Content-Type: application/json" \
     *   -d '{"to":"user@example.com","subject":"Test","body":"Hello!"}'
     */
    @PostMapping("/send-email")
    public String sendManualEmail(@RequestBody EmailRequest request) {
        System.out.println("📧 Manual email request received");

        try {
            emailSender.sendTaskCreatedEmail(
                    request.getSubject() != null ? request.getSubject() : "Manual Test",
                    request.getBody() != null ? request.getBody() : "Test email body",
                    "manual-test-id"
            );

            return "Email sent successfully (check console or SMTP)";

        } catch (Exception e) {
            System.err.println("❌ Error sending manual email: " + e.getMessage());
            return "Error sending email: " + e.getMessage();
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public String health() {
        return "Email Service is running! 📧";
    }
}