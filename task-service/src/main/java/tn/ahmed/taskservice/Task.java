package tn.ahmed.taskservice;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Task {
    @JsonProperty("id")
    private String id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("description")
    private String description;

    // ═══════════════════════════════════════════════════════════
    // User & Team Context
    // ═══════════════════════════════════════════════════════════
    @JsonProperty("assignedTo")
    private String assignedTo;  // User ID or email

    @JsonProperty("teamId")
    private String teamId;  // Team identifier

    @JsonProperty("createdBy")
    private String createdBy;  // User who created the task

    // ═══════════════════════════════════════════════════════════
    // Status & Priority
    // ═══════════════════════════════════════════════════════════
    @JsonProperty("status")
    @Builder.Default
    private TaskStatus status = TaskStatus.OPEN;

    @JsonProperty("priority")
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    // ═══════════════════════════════════════════════════════════
    // Timestamps (Fixed with @JsonFormat)
    // ═══════════════════════════════════════════════════════════
    @JsonProperty("createdAt")
    private String createdAt;

    @JsonProperty("dueDate")
    private String dueDate;

    @JsonProperty("updatedAt")
    private String updatedAt;

    // ═══════════════════════════════════════════════════════════
    // Metadata
    // ═══════════════════════════════════════════════════════════
    @JsonProperty("tags")
    private String[] tags;  // e.g., ["bug", "urgent", "frontend"]

    @JsonProperty("estimatedHours")
    private Double estimatedHours;

    @JsonProperty("actualHours")
    private Double actualHours;
}

// ═══════════════════════════════════════════════════════════
// Enums for Status and Priority
// ═══════════════════════════════════════════════════════════
enum TaskStatus {
    OPEN,
    IN_PROGRESS,
    IN_REVIEW,
    COMPLETED,
    CANCELLED
}

enum Priority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}