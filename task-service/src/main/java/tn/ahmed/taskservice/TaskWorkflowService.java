package tn.ahmed.taskservice;

import io.dapr.client.DaprClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Task Workflow Service
 * Manages task state transitions and business rules
 */
@Service
public class TaskWorkflowService {

    @Autowired
    private DaprClient client;

    private final String STATE_STORE = "statestore";
    private final String PUBSUB_NAME = "pubsub";

    // ═══════════════════════════════════════════════════════════
    // Valid State Transitions
    // ═══════════════════════════════════════════════════════════
    private static final Map<TaskStatus, TaskStatus[]> VALID_TRANSITIONS = Map.of(
            TaskStatus.OPEN, new TaskStatus[]{TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED},
            TaskStatus.IN_PROGRESS, new TaskStatus[]{TaskStatus.IN_REVIEW, TaskStatus.OPEN, TaskStatus.CANCELLED},
            TaskStatus.IN_REVIEW, new TaskStatus[]{TaskStatus.COMPLETED, TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED},
            TaskStatus.COMPLETED, new TaskStatus[]{TaskStatus.IN_PROGRESS}, // Reopen if needed
            TaskStatus.CANCELLED, new TaskStatus[]{TaskStatus.OPEN} // Can reopen cancelled tasks
    );

    /**
     * Transition task to new status
     */
    public Task transitionStatus(String taskId, TaskStatus newStatus, String userId) throws Exception {
        // Get current task
        var state = client.getState(STATE_STORE, taskId, Task.class).block();
        if (state == null || state.getValue() == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }

        Task task = state.getValue();
        TaskStatus oldStatus = task.getStatus();

        // Validate transition
        if (!isValidTransition(oldStatus, newStatus)) {
            throw new IllegalStateException(
                    String.format("Invalid transition from %s to %s", oldStatus, newStatus)
            );
        }

        // Update task
        task.setStatus(newStatus);
        task.setUpdatedAt(LocalDateTime.now().toString());

        // Save to state store
        client.saveState(STATE_STORE, taskId, task).block();

        // Publish status change event
        publishStatusChangeEvent(task, oldStatus, newStatus, userId);

        System.out.println(String.format("✅ Task %s: %s → %s (by %s)",
                taskId, oldStatus, newStatus, userId));

        return task;
    }

    /**
     * Assign task to user
     */
    public Task assignTask(String taskId, String assignedTo, String assignedBy) throws Exception {
        var state = client.getState(STATE_STORE, taskId, Task.class).block();
        if (state == null || state.getValue() == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }

        Task task = state.getValue();
        String oldAssignee = task.getAssignedTo();

        task.setAssignedTo(assignedTo);
        task.setUpdatedAt(LocalDateTime.now().toString());

        // If task is OPEN, auto-transition to IN_PROGRESS
        if (task.getStatus() == TaskStatus.OPEN) {
            task.setStatus(TaskStatus.IN_PROGRESS);
        }

        client.saveState(STATE_STORE, taskId, task).block();

        // Publish assignment event
        publishAssignmentEvent(task, oldAssignee, assignedTo, assignedBy);

        System.out.println(String.format("👤 Task %s assigned: %s → %s (by %s)",
                taskId, oldAssignee, assignedTo, assignedBy));

        return task;
    }

    /**
     * Update task priority
     */
    public Task updatePriority(String taskId, Priority newPriority, String userId) throws Exception {
        var state = client.getState(STATE_STORE, taskId, Task.class).block();
        if (state == null || state.getValue() == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }

        Task task = state.getValue();
        Priority oldPriority = task.getPriority();

        task.setPriority(newPriority);
        task.setUpdatedAt(LocalDateTime.now().toString());

        client.saveState(STATE_STORE, taskId, task).block();

        // Publish priority change event (for notifications)
        if (newPriority == Priority.URGENT || newPriority == Priority.HIGH) {
            publishPriorityChangeEvent(task, oldPriority, newPriority, userId);
        }

        System.out.println(String.format("⚡ Task %s priority: %s → %s",
                taskId, oldPriority, newPriority));

        return task;
    }

    /**
     * Check if transition is valid
     */
    private boolean isValidTransition(TaskStatus from, TaskStatus to) {
        if (from == to) return true; // Same status is valid (idempotent)

        TaskStatus[] validTargets = VALID_TRANSITIONS.get(from);
        if (validTargets == null) return false;

        for (TaskStatus valid : validTargets) {
            if (valid == to) return true;
        }
        return false;
    }

    /**
     * Publish status change event
     */
    private void publishStatusChangeEvent(Task task, TaskStatus oldStatus,
                                          TaskStatus newStatus, String userId) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "TaskStatusChanged");
            event.put("taskId", task.getId());
            event.put("title", task.getTitle());
            event.put("oldStatus", oldStatus.toString());
            event.put("newStatus", newStatus.toString());
            event.put("changedBy", userId);
            event.put("timestamp", LocalDateTime.now().toString());
            event.put("priority", task.getPriority().toString());
            event.put("assignedTo", task.getAssignedTo());

            client.publishEvent(PUBSUB_NAME, "task-status-changed", event).block();
        } catch (Exception e) {
            System.err.println("Failed to publish status change event: " + e.getMessage());
        }
    }

    /**
     * Publish assignment event
     */
    private void publishAssignmentEvent(Task task, String oldAssignee,
                                        String newAssignee, String assignedBy) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "TaskAssigned");
            event.put("taskId", task.getId());
            event.put("title", task.getTitle());
            event.put("oldAssignee", oldAssignee);
            event.put("newAssignee", newAssignee);
            event.put("assignedBy", assignedBy);
            event.put("timestamp", LocalDateTime.now().toString());
            event.put("priority", task.getPriority().toString());

            client.publishEvent(PUBSUB_NAME, "task-assigned", event).block();
        } catch (Exception e) {
            System.err.println("Failed to publish assignment event: " + e.getMessage());
        }
    }

    /**
     * Publish priority change event
     */
    private void publishPriorityChangeEvent(Task task, Priority oldPriority,
                                            Priority newPriority, String userId) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "TaskPriorityChanged");
            event.put("taskId", task.getId());
            event.put("title", task.getTitle());
            event.put("oldPriority", oldPriority.toString());
            event.put("newPriority", newPriority.toString());
            event.put("changedBy", userId);
            event.put("timestamp", LocalDateTime.now().toString());
            event.put("assignedTo", task.getAssignedTo());

            client.publishEvent(PUBSUB_NAME, "task-priority-changed", event).block();
        } catch (Exception e) {
            System.err.println("Failed to publish priority change event: " + e.getMessage());
        }
    }
}