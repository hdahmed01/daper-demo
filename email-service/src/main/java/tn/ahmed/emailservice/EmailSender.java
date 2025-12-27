package tn.ahmed.emailservice;

import io.dapr.client.DaprClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Email Sender Service
 *
 * Handles email sending logic using:
 * - Mock mode: Console logging (for testing)
 * - SMTP mode: Real emails via Dapr Output Binding
 */
@Service
public class EmailSender {

    @Autowired
    private DaprClient client;

    @Value("${email.mode:mock}")
    private String emailMode;

    private static final String EMAIL_BINDING = "email-binding";
    private static final String SECRET_STORE = "local-secret-store";

    /**
     * Send email notification
     *
     * @param taskTitle Task title
     * @param taskDescription Task description
     * @param taskId Task ID
     */
    public void sendTaskCreatedEmail(String taskTitle, String taskDescription, String taskId) {

        if ("mock".equalsIgnoreCase(emailMode)) {
            sendMockEmail(taskTitle, taskDescription, taskId);
        } else {
            sendRealEmail(taskTitle, taskDescription, taskId);
        }
    }

    /**
     * ════════════════════════════════════════════════════════
     * MOCK MODE - Console logging (no real email sent)
     * ════════════════════════════════════════════════════════
     */
    private void sendMockEmail(String taskTitle, String taskDescription, String taskId) {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("📧 MOCK EMAIL - Would send:");
        System.out.println("   To: user@example.com");
        System.out.println("   Subject: New Task Created: " + taskTitle);
        System.out.println("   Body:");
        System.out.println("   ┌─────────────────────────────────────────┐");
        System.out.println("   │ Task Created Successfully!              │");
        System.out.println("   │                                         │");
        System.out.println("   │ Task ID: " + taskId + "                │");
        System.out.println("   │ Title: " + taskTitle + "                │");
        System.out.println("   │ Description: " + taskDescription + "   │");
        System.out.println("   │                                         │");
        System.out.println("   │ Manage your tasks at:                   │");
        System.out.println("   │ http://localhost:8081/tasks/" + taskId + " │");
        System.out.println("   └─────────────────────────────────────────┘");
        System.out.println("═══════════════════════════════════════════════════");
    }

    /**
     * ════════════════════════════════════════════════════════
     * SMTP MODE - Real email via Dapr Output Binding
     * ════════════════════════════════════════════════════════
     *
     * Uses:
     * 1. Dapr Secrets to get SMTP credentials (optional)
     * 2. Dapr Output Binding to send email via SMTP
     */
    private void sendRealEmail(String taskTitle, String taskDescription, String taskId) {
        System.out.println("📧 Sending real email via SMTP...");

        try {
            // ──────────────────────────────────────────────────
            // STEP 1: Retrieve secrets (optional - for demo)
            // ──────────────────────────────────────────────────
            // In production, you'd get recipient email from secrets
            // var secrets = client.getSecret(SECRET_STORE, "email-config").block();
            // String recipientEmail = secrets.get("recipient-email");

            String recipientEmail = "user@example.com"; // Hardcoded for demo

            // ──────────────────────────────────────────────────
            // STEP 2: Prepare email metadata
            // ──────────────────────────────────────────────────
            Map<String, String> metadata = new HashMap<>();
            metadata.put("emailTo", recipientEmail);
            metadata.put("emailFrom", "noreply@taskmanagement.com");
            metadata.put("subject", "New Task Created: " + taskTitle);

            // ──────────────────────────────────────────────────
            // STEP 3: Prepare email body
            // ──────────────────────────────────────────────────
            String emailBody = buildEmailBody(taskTitle, taskDescription, taskId);

            // ──────────────────────────────────────────────────
            // STEP 4: Send via Dapr Output Binding
            // ──────────────────────────────────────────────────
            // Dapr will handle SMTP connection, authentication, retry


            client.invokeBinding(
                    EMAIL_BINDING,
                    "create",
                    emailBody.getBytes(),  // ✅ byte[] - correct !
                    metadata
            ).block();

            System.out.println("✅ Email sent successfully via SMTP!");
            System.out.println("   To: " + recipientEmail);
            System.out.println("   Subject: New Task Created: " + taskTitle);

        } catch (Exception e) {
            System.err.println("❌ Failed to send email: " + e.getMessage());
            e.printStackTrace();

            // Fallback to mock
            System.out.println("⚠️  Falling back to mock email...");
            sendMockEmail(taskTitle, taskDescription, taskId);
        }
    }

    /**
     * Build HTML email body
     */
    private String buildEmailBody(String title, String description, String taskId) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif;">
                <h2>🎉 New Task Created!</h2>
                <div style="background: #f5f5f5; padding: 20px; border-radius: 5px;">
                    <p><strong>Task ID:</strong> %s</p>
                    <p><strong>Title:</strong> %s</p>
                    <p><strong>Description:</strong> %s</p>
                </div>
                <p style="margin-top: 20px;">
                    <a href="http://localhost:8081/tasks/%s" 
                       style="background: #007bff; color: white; padding: 10px 20px; 
                              text-decoration: none; border-radius: 5px;">
                        View Task
                    </a>
                </p>
                <hr/>
                <p style="color: #666; font-size: 12px;">
                    This is an automated notification from Task Management System
                </p>
            </body>
            </html>
            """, taskId, title, description, taskId);
    }
}