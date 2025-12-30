package tn.ahmed.taskservice.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "tasks")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Task {
    @Id
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



