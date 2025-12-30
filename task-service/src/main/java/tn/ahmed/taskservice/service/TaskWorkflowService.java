package tn.ahmed.taskservice.service;

import io.dapr.client.DaprClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.ahmed.taskservice.entities.Priority;
import tn.ahmed.taskservice.entities.Task;
import tn.ahmed.taskservice.entities.TaskStatus;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class TaskWorkflowService {

    @Autowired
    private DaprClient client;

    @Autowired
    private TaskService taskService;

    private final String PUBSUB_NAME = "pubsub";

    private static final Map<TaskStatus, TaskStatus[]> VALID_TRANSITIONS = Map.of(
            TaskStatus.OPEN, new TaskStatus[]{TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED},
            TaskStatus.IN_PROGRESS, new TaskStatus[]{TaskStatus.IN_REVIEW, TaskStatus.OPEN, TaskStatus.CANCELLED},
            TaskStatus.IN_REVIEW, new TaskStatus[]{TaskStatus.COMPLETED, TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED},
            TaskStatus.COMPLETED, new TaskStatus[]{TaskStatus.IN_PROGRESS},
            TaskStatus.CANCELLED, new TaskStatus[]{TaskStatus.OPEN}
    );

    public Task transitionStatus(String taskId, TaskStatus newStatus, String userId) throws Exception {
        Task task = taskService.getTask(taskId);
        TaskStatus oldStatus = task.getStatus();

        if (!isValidTransition(oldStatus, newStatus)) {
            throw new IllegalStateException(
                    String.format("Invalid transition from %s to %s", oldStatus, newStatus)
            );
        }

        task.setStatus(newStatus);
        task = taskService.updateTask(taskId, task, userId);

        publishStatusChangeEvent(task, oldStatus, newStatus, userId);

        System.out.println(String.format("✅ Status changed: %s → %s", oldStatus, newStatus));
        return task;
    }

    public Task assignTask(String taskId, String assignedTo, String assignedBy) throws Exception {
        Task task = taskService.getTask(taskId);
        String oldAssignee = task.getAssignedTo();

        task.setAssignedTo(assignedTo);

        if (task.getStatus() == TaskStatus.OPEN) {
            task.setStatus(TaskStatus.IN_PROGRESS);
        }

        task = taskService.updateTask(taskId, task, assignedBy);

        publishAssignmentEvent(task, oldAssignee, assignedTo, assignedBy);

        System.out.println(String.format("👤 Assigned: %s → %s", oldAssignee, assignedTo));
        return task;
    }

    public Task updatePriority(String taskId, Priority newPriority, String userId) throws Exception {
        Task task = taskService.getTask(taskId);
        Priority oldPriority = task.getPriority();

        task.setPriority(newPriority);
        task = taskService.updateTask(taskId, task, userId);

        if (newPriority == Priority.URGENT || newPriority == Priority.HIGH) {
            publishPriorityChangeEvent(task, oldPriority, newPriority, userId);
        }

        System.out.println(String.format("⚡ Priority: %s → %s", oldPriority, newPriority));
        return task;
    }

    private boolean isValidTransition(TaskStatus from, TaskStatus to) {
        if (from == to) return true;
        TaskStatus[] validTargets = VALID_TRANSITIONS.get(from);
        if (validTargets == null) return false;
        for (TaskStatus valid : validTargets) {
            if (valid == to) return true;
        }
        return false;
    }

    private void publishStatusChangeEvent(Task task, TaskStatus oldStatus, TaskStatus newStatus, String userId) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("taskId", task.getId());
            event.put("title", task.getTitle());
            event.put("oldStatus", oldStatus.toString());
            event.put("newStatus", newStatus.toString());
            event.put("changedBy", userId);
            event.put("timestamp", LocalDateTime.now().toString());
            client.publishEvent(PUBSUB_NAME, "task-status-changed", event).block();
        } catch (Exception e) {
            System.err.println("Event publish error: " + e.getMessage());
        }
    }

    private void publishAssignmentEvent(Task task, String oldAssignee, String newAssignee, String assignedBy) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("taskId", task.getId());
            event.put("title", task.getTitle());
            event.put("oldAssignee", oldAssignee);
            event.put("newAssignee", newAssignee);
            event.put("assignedBy", assignedBy);
            event.put("timestamp", LocalDateTime.now().toString());
            client.publishEvent(PUBSUB_NAME, "task-assigned", event).block();
        } catch (Exception e) {
            System.err.println("Event publish error: " + e.getMessage());
        }
    }

    private void publishPriorityChangeEvent(Task task, Priority oldPriority, Priority newPriority, String userId) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("taskId", task.getId());
            event.put("title", task.getTitle());
            event.put("oldPriority", oldPriority.toString());
            event.put("newPriority", newPriority.toString());
            event.put("changedBy", userId);
            event.put("timestamp", LocalDateTime.now().toString());
            client.publishEvent(PUBSUB_NAME, "task-priority-changed", event).block();
        } catch (Exception e) {
            System.err.println("Event publish error: " + e.getMessage());
        }
    }
}