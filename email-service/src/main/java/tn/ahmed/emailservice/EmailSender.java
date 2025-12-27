package tn.ahmed.emailservice;

import io.dapr.client.DaprClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Email Sender Service with MailHog Support
 *
 * Modes:
 * - mock: Console logging only
 * - smtp: Real emails via MailHog/SMTP
 */
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
     * Send task creation email notification
     */
    public void sendTaskCreatedEmail(String taskTitle, String taskDescription, String taskId) {
        if ("mock".equalsIgnoreCase(emailMode)) {
            sendMockEmail(taskTitle, taskDescription, taskId);
        } else {
            sendRealEmail(taskTitle, taskDescription, taskId);
        }
    }

    /**
     * MOCK MODE - Console logging
     */
    private void sendMockEmail(String taskTitle, String taskDescription, String taskId) {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("📧 MOCK EMAIL - Would send:");
        System.out.println("   To: " + defaultRecipient);
        System.out.println("   Subject: New Task Created: " + taskTitle);
        System.out.println("   Task ID: " + taskId);
        System.out.println("   Title: " + taskTitle);
        System.out.println("   Description: " + taskDescription);
        System.out.println("═══════════════════════════════════════════════════");
    }

    /**
     * SMTP MODE - Real emails via MailHog
     */
    private void sendRealEmail(String taskTitle, String taskDescription, String taskId) {
        System.out.println("📧 Sending email via MailHog SMTP...");

        try {
            // Prepare email metadata
            Map<String, String> metadata = new HashMap<>();
            metadata.put("emailTo", defaultRecipient);
            metadata.put("emailFrom", "taskmanagement@example.com");
            metadata.put("subject", "✅ New Task Created: " + taskTitle);

            // Build HTML email body
            String emailBody = buildEmailBody(taskTitle, taskDescription, taskId);

            // Send via Dapr SMTP Binding
            client.invokeBinding(
                    EMAIL_BINDING,
                    "create",
                    emailBody.getBytes(),
                    metadata
            ).block();

            System.out.println("✅ Email sent successfully via MailHog!");
            System.out.println("   To: " + defaultRecipient);
            System.out.println("   Subject: New Task Created: " + taskTitle);
            System.out.println("   View at: http://localhost:8025");

        } catch (Exception e) {
            System.err.println("❌ Failed to send email: " + e.getMessage());
            e.printStackTrace();

            // Fallback to mock
            System.out.println("⚠️  Falling back to mock email...");
            sendMockEmail(taskTitle, taskDescription, taskId);
        }
    }

    /**
     * Build professional HTML email
     */
    private String buildEmailBody(String title, String description, String taskId) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); 
                              color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                    .task-box { background: white; padding: 20px; margin: 20px 0; 
                               border-left: 4px solid #667eea; border-radius: 5px; }
                    .task-label { font-weight: bold; color: #667eea; margin-bottom: 5px; }
                    .task-value { color: #555; margin-bottom: 15px; }
                    .button { display: inline-block; background: #667eea; color: white; 
                             padding: 12px 30px; text-decoration: none; border-radius: 5px; 
                             margin-top: 20px; }
                    .footer { text-align: center; color: #999; font-size: 12px; 
                             margin-top: 30px; padding-top: 20px; border-top: 1px solid #ddd; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🎉 New Task Created!</h1>
                        <p>A new task has been added to your task management system</p>
                    </div>
                    <div class="content">
                        <div class="task-box">
                            <div class="task-label">📋 Task ID</div>
                            <div class="task-value">%s</div>
                            
                            <div class="task-label">✏️ Title</div>
                            <div class="task-value">%s</div>
                            
                            <div class="task-label">📝 Description</div>
                            <div class="task-value">%s</div>
                        </div>
                        
                        <a href="http://localhost:8081/tasks/%s" class="button">
                            View Task Details →
                        </a>
                    </div>
                    <div class="footer">
                        <p>This is an automated notification from Task Management System</p>
                        <p>Powered by Dapr + MailHog</p>
                    </div>
                </div>
            </body>
            </html>
            """, taskId, title, description, taskId);
    }
}