package tn.ahmed.emailservice;

import io.dapr.client.DaprClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class EmailSender {

    @Autowired
    private DaprClient client;

    @Value("${email.mode:smtp}")
    private String emailMode;

    @Value("${email.recipient:user@example.com}")
    private String defaultRecipient;

    private static final String EMAIL_BINDING = "email-binding";

    /**
     * Generic send email method (for smart notifications)
     */
    public void sendEmail(String to, String subject, String htmlBody) {
        if ("mock".equalsIgnoreCase(emailMode)) {
            sendMockEmail(to, subject, htmlBody);
        } else {
            sendRealEmail(to, subject, htmlBody);
        }
    }

    /**
     * Legacy method for backward compatibility
     */
    public void sendTaskCreatedEmail(String taskTitle, String taskDescription, String taskId) {
        String subject = "New Task Created: " + taskTitle;
        String body = buildSimpleTaskEmail(taskTitle, taskDescription, taskId);
        sendEmail(defaultRecipient, subject, body);
    }

    /**
     * MOCK MODE - Console logging
     */
    private void sendMockEmail(String to, String subject, String body) {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("📧 MOCK EMAIL");
        System.out.println("   To: " + to);
        System.out.println("   Subject: " + subject);
        System.out.println("   Body: " + (body.length() > 200 ? body.substring(0, 200) + "..." : body));
        System.out.println("═══════════════════════════════════════════════════");
    }

    /**
     * SMTP MODE - Real emails via MailHog
     */
    private void sendRealEmail(String to, String subject, String htmlBody) {
        System.out.println("📧 Sending email via MailHog SMTP...");

        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("emailTo", to);
            metadata.put("emailFrom", "taskmanagement@example.com");
            metadata.put("subject", subject);

            client.invokeBinding(
                    EMAIL_BINDING,
                    "create",
                    htmlBody.getBytes(),
                    metadata
            ).block();

            System.out.println("✅ Email sent successfully!");
            System.out.println("   To: " + to);
            System.out.println("   Subject: " + subject);
            System.out.println("   View at: http://localhost:8025");

        } catch (Exception e) {
            System.err.println("❌ Failed to send email: " + e.getMessage());
            e.printStackTrace();
            // Fallback to mock
            System.out.println("⚠️  Falling back to mock email...");
            sendMockEmail(to, subject, htmlBody);
        }
    }

    /**
     * Simple email template
     */
    private String buildSimpleTaskEmail(String title, String description, String taskId) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <body style="font-family: Arial, sans-serif;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #667eea;">New Task Created</h2>
                    <div style="background: #f9f9f9; padding: 20px; border-radius: 8px; margin: 20px 0;">
                        <h3>%s</h3>
                        <p><strong>Description:</strong> %s</p>
                        <p><strong>Task ID:</strong> %s</p>
                    </div>
                    <a href="http://localhost:8081/tasks/%s" 
                       style="background: #667eea; color: white; padding: 12px 24px; 
                              text-decoration: none; border-radius: 5px; display: inline-block;">
                        View Task
                    </a>
                </div>
            </body>
            </html>
            """, title, description, taskId, taskId);
    }
}