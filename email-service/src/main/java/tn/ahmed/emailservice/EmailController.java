package tn.ahmed.emailservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.client.domain.CloudEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Smart Email Controller with Intelligent Notification Rules
 *
 * Rules:
 * 1. URGENT/HIGH priority tasks → Immediate email
 * 2. Task assignments → Notify assignee
 * 3. Status changes to COMPLETED → Notify creator
 * 4. Status changes to IN_REVIEW → Notify team lead
 * 5. Regular tasks → Batched in digest (future feature)
 */
@RestController
public class EmailController {

    @Autowired
    private EmailSender emailSender;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ═══════════════════════════════════════════════════════════
    // TASK CREATED - Smart filtering based on priority
    // ═══════════════════════════════════════════════════════════
    @PostMapping("/task-created")
    public void onTaskCreated(@RequestBody CloudEvent event) {
        System.out.println("📧 Email Service: Received task-created event");

        try {
            Map<String, Object> data = extractEventData(event);

            String taskId = getString(data, "id");
            String title = getString(data, "title");
            String description = getString(data, "description");
            String priority = getString(data, "priority");
            String assignedTo = getString(data, "assignedTo");

            System.out.println(String.format("   Task: %s [Priority: %s]", title, priority));

            // ──────────────────────────────────────────────────────
            // RULE 1: Only send immediate email for HIGH/URGENT
            // ──────────────────────────────────────────────────────
            if ("HIGH".equals(priority) || "URGENT".equals(priority)) {
                System.out.println("   🚨 High/Urgent priority - sending immediate notification");

                String subject = String.format("⚡ %s Priority Task Created: %s", priority, title);
                String body = buildPriorityTaskEmail(title, description, taskId, priority, assignedTo);

                emailSender.sendEmail(assignedTo != null ? assignedTo : "team@example.com", subject, body);
            } else {
                System.out.println("   ℹ️  Normal priority - queued for digest (future feature)");
                // Future: Add to digest queue
            }

        } catch (Exception e) {
            System.err.println("❌ Error processing task-created: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════════════════════════════════
    // TASK ASSIGNED - Notify new assignee
    // ═══════════════════════════════════════════════════════════
    @PostMapping("/task-assigned")
    public void onTaskAssigned(@RequestBody CloudEvent event) {
        System.out.println("📧 Email Service: Received task-assigned event");

        try {
            Map<String, Object> data = extractEventData(event);

            String taskId = getString(data, "taskId");
            String title = getString(data, "title");
            String newAssignee = getString(data, "newAssignee");
            String assignedBy = getString(data, "assignedBy");
            String priority = getString(data, "priority");

            System.out.println(String.format("   Task %s assigned to %s", taskId, newAssignee));

            // ──────────────────────────────────────────────────────
            // RULE 2: Always notify assignee
            // ──────────────────────────────────────────────────────
            String subject = String.format("👤 Task Assigned to You: %s", title);
            String body = buildAssignmentEmail(title, taskId, assignedBy, priority);

            emailSender.sendEmail(newAssignee, subject, body);

        } catch (Exception e) {
            System.err.println("❌ Error processing task-assigned: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════════════════════════════════
    // TASK STATUS CHANGED - Conditional notifications
    // ═══════════════════════════════════════════════════════════
    @PostMapping("/task-status-changed")
    public void onTaskStatusChanged(@RequestBody CloudEvent event) {
        System.out.println("📧 Email Service: Received task-status-changed event");

        try {
            Map<String, Object> data = extractEventData(event);

            String taskId = getString(data, "taskId");
            String title = getString(data, "title");
            String oldStatus = getString(data, "oldStatus");
            String newStatus = getString(data, "newStatus");
            String assignedTo = getString(data, "assignedTo");
            String changedBy = getString(data, "changedBy");

            System.out.println(String.format("   Task %s: %s → %s", taskId, oldStatus, newStatus));

            // ──────────────────────────────────────────────────────
            // RULE 3: Notify on COMPLETED (to creator/stakeholders)
            // ──────────────────────────────────────────────────────
            if ("COMPLETED".equals(newStatus)) {
                String subject = String.format("✅ Task Completed: %s", title);
                String body = buildCompletionEmail(title, taskId, changedBy);

                // In production: notify creator, stakeholders, team lead
                emailSender.sendEmail("team@example.com", subject, body);
            }

            // ──────────────────────────────────────────────────────
            // RULE 4: Notify on IN_REVIEW (to reviewers)
            // ──────────────────────────────────────────────────────
            else if ("IN_REVIEW".equals(newStatus)) {
                String subject = String.format("👀 Task Ready for Review: %s", title);
                String body = buildReviewEmail(title, taskId, assignedTo);

                // In production: notify team lead or designated reviewers
                emailSender.sendEmail("lead@example.com", subject, body);
            }

        } catch (Exception e) {
            System.err.println("❌ Error processing task-status-changed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════════════════════════════════
    // TASK PRIORITY CHANGED - Notify on escalation
    // ═══════════════════════════════════════════════════════════
    @PostMapping("/task-priority-changed")
    public void onTaskPriorityChanged(@RequestBody CloudEvent event) {
        System.out.println("📧 Email Service: Received task-priority-changed event");

        try {
            Map<String, Object> data = extractEventData(event);

            String taskId = getString(data, "taskId");
            String title = getString(data, "title");
            String oldPriority = getString(data, "oldPriority");
            String newPriority = getString(data, "newPriority");
            String assignedTo = getString(data, "assignedTo");
            String changedBy = getString(data, "changedBy");

            System.out.println(String.format("   Task %s: %s → %s priority",
                    taskId, oldPriority, newPriority));

            // ──────────────────────────────────────────────────────
            // RULE 5: Only notify on escalation to HIGH/URGENT
            // ──────────────────────────────────────────────────────
            if (("HIGH".equals(newPriority) || "URGENT".equals(newPriority))) {
                String subject = String.format("⚡ Priority Escalated: %s", title);
                String body = buildPriorityEscalationEmail(title, taskId, oldPriority,
                        newPriority, changedBy);

                emailSender.sendEmail(assignedTo != null ? assignedTo : "team@example.com",
                        subject, body);
            }

        } catch (Exception e) {
            System.err.println("❌ Error processing task-priority-changed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════

    private Map<String, Object> extractEventData(CloudEvent event) {
        Object rawData = event.getData();
        if (rawData instanceof Map) {
            return (Map<String, Object>) rawData;
        }
        return objectMapper.convertValue(rawData, Map.class);
    }

    private String getString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? String.valueOf(value) : null;
    }

    // ═══════════════════════════════════════════════════════════
    // EMAIL TEMPLATES
    // ═══════════════════════════════════════════════════════════

    private String buildPriorityTaskEmail(String title, String description,
                                          String taskId, String priority, String assignedTo) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif;">
                <div style="background: #ff5252; color: white; padding: 20px; border-radius: 8px;">
                    <h2>⚡ %s Priority Task</h2>
                </div>
                <div style="padding: 20px;">
                    <h3>%s</h3>
                    <p><strong>Description:</strong> %s</p>
                    <p><strong>Task ID:</strong> %s</p>
                    <p><strong>Assigned To:</strong> %s</p>
                    <p><strong>Priority:</strong> <span style="color: #ff5252; font-weight: bold;">%s</span></p>
                    <a href="http://localhost:8081/tasks/%s" 
                       style="background: #4285f4; color: white; padding: 10px 20px; 
                              text-decoration: none; border-radius: 4px; display: inline-block; margin-top: 15px;">
                        View Task
                    </a>
                </div>
            </body>
            </html>
            """, priority, title, description, taskId,
                assignedTo != null ? assignedTo : "Unassigned", priority, taskId);
    }

    private String buildAssignmentEmail(String title, String taskId,
                                        String assignedBy, String priority) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif;">
                <div style="background: #4285f4; color: white; padding: 20px; border-radius: 8px;">
                    <h2>👤 New Task Assignment</h2>
                </div>
                <div style="padding: 20px;">
                    <h3>%s</h3>
                    <p>You have been assigned a new task by <strong>%s</strong></p>
                    <p><strong>Task ID:</strong> %s</p>
                    <p><strong>Priority:</strong> %s</p>
                    <a href="http://localhost:8081/tasks/%s" 
                       style="background: #4285f4; color: white; padding: 10px 20px; 
                              text-decoration: none; border-radius: 4px; display: inline-block; margin-top: 15px;">
                        View Task Details
                    </a>
                </div>
            </body>
            </html>
            """, title, assignedBy, taskId, priority, taskId);
    }

    private String buildCompletionEmail(String title, String taskId, String completedBy) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif;">
                <div style="background: #34a853; color: white; padding: 20px; border-radius: 8px;">
                    <h2>✅ Task Completed</h2>
                </div>
                <div style="padding: 20px;">
                    <h3>%s</h3>
                    <p>This task has been marked as completed by <strong>%s</strong></p>
                    <p><strong>Completed:</strong> %s</p>
                    <a href="http://localhost:8081/tasks/%s" 
                       style="background: #34a853; color: white; padding: 10px 20px; 
                              text-decoration: none; border-radius: 4px; display: inline-block; margin-top: 15px;">
                        View Task
                    </a>
                </div>
            </body>
            </html>
            """, title, completedBy, LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")), taskId);
    }

    private String buildReviewEmail(String title, String taskId, String assignedTo) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif;">
                <div style="background: #fbbc04; color: white; padding: 20px; border-radius: 8px;">
                    <h2>👀 Task Ready for Review</h2>
                </div>
                <div style="padding: 20px;">
                    <h3>%s</h3>
                    <p>Submitted by: <strong>%s</strong></p>
                    <p>Please review this task at your earliest convenience.</p>
                    <a href="http://localhost:8081/tasks/%s" 
                       style="background: #fbbc04; color: white; padding: 10px 20px; 
                              text-decoration: none; border-radius: 4px; display: inline-block; margin-top: 15px;">
                        Review Task
                    </a>
                </div>
            </body>
            </html>
            """, title, assignedTo, taskId);
    }

    private String buildPriorityEscalationEmail(String title, String taskId,
                                                String oldPriority, String newPriority, String changedBy) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif;">
                <div style="background: #ff5252; color: white; padding: 20px; border-radius: 8px;">
                    <h2>⚡ Priority Escalated</h2>
                </div>
                <div style="padding: 20px;">
                    <h3>%s</h3>
                    <p>Priority changed by <strong>%s</strong></p>
                    <p><strong>%s</strong> → <strong style="color: #ff5252;">%s</strong></p>
                    <p style="background: #fff3cd; padding: 15px; border-left: 4px solid #ff5252;">
                        ⚠️ This task now requires immediate attention!
                    </p>
                    <a href="http://localhost:8081/tasks/%s" 
                       style="background: #ff5252; color: white; padding: 10px 20px; 
                              text-decoration: none; border-radius: 4px; display: inline-block; margin-top: 15px;">
                        View Task
                    </a>
                </div>
            </body>
            </html>
            """, title, changedBy, oldPriority, newPriority, taskId);
    }

    @GetMapping("/health")
    public String health() {
        return "Smart Email Service is running! 📧";
    }
}