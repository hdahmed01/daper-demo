package tn.ahmed.taskservice;

import io.dapr.client.DaprClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    @Autowired
    private DaprClient client;

    @Autowired
    private TaskWorkflowService workflowService;

    private final String STATE_STORE = "statestore";
    private final String PUBSUB_NAME = "pubsub";

    // ═══════════════════════════════════════════════════════════
    // CREATE TASK (Enhanced)
    // ═══════════════════════════════════════════════════════════
    @PostMapping
    public ResponseEntity<Task> createTask(@RequestBody Task task,
                                           @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        try {
            // Generate ID and set metadata
            task.setId(UUID.randomUUID().toString());
            task.setCreatedAt(LocalDateTime.now().toString());
            task.setUpdatedAt(LocalDateTime.now().toString());
            task.setCreatedBy(userId);

            // Default status if not set
            if (task.getStatus() == null) {
                task.setStatus(TaskStatus.OPEN);
            }
            if (task.getPriority() == null) {
                task.setPriority(Priority.MEDIUM);
            }

            // Save to state store
            client.saveState(STATE_STORE, task.getId(), task).block();

            // Publish task-created event
            client.publishEvent(PUBSUB_NAME, "task-created", task).block();

            System.out.println(String.format("✅ Task created: %s [%s] by %s",
                    task.getId(), task.getTitle(), userId));

            return ResponseEntity.ok(task);

        } catch (Exception e) {
            System.err.println("Error creating task: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // ═══════════════════════════════════════════════════════════
    // GET TASK BY ID
    // ═══════════════════════════════════════════════════════════
    @GetMapping("/{id}")
    public ResponseEntity<Task> getTask(@PathVariable String id) {
        try {
            var state = client.getState(STATE_STORE, id, Task.class).block();
            if (state == null || state.getValue() == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(state.getValue());
        } catch (Exception e) {
            System.err.println("Error getting task: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // ═══════════════════════════════════════════════════════════
    // UPDATE TASK STATUS (Workflow)
    // ═══════════════════════════════════════════════════════════
    @PatchMapping("/{id}/status")
    public ResponseEntity<Task> updateStatus(@PathVariable String id,
                                             @RequestParam TaskStatus status,
                                             @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        try {
            Task updatedTask = workflowService.transitionStatus(id, status, userId);
            return ResponseEntity.ok(updatedTask);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            System.err.println("Error updating status: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // ═══════════════════════════════════════════════════════════
    // ASSIGN TASK
    // ═══════════════════════════════════════════════════════════
    @PatchMapping("/{id}/assign")
    public ResponseEntity<Task> assignTask(@PathVariable String id,
                                           @RequestParam String assignedTo,
                                           @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        try {
            Task updatedTask = workflowService.assignTask(id, assignedTo, userId);
            return ResponseEntity.ok(updatedTask);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.err.println("Error assigning task: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // ═══════════════════════════════════════════════════════════
    // UPDATE PRIORITY
    // ═══════════════════════════════════════════════════════════
    @PatchMapping("/{id}/priority")
    public ResponseEntity<Task> updatePriority(@PathVariable String id,
                                               @RequestParam Priority priority,
                                               @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        try {
            Task updatedTask = workflowService.updatePriority(id, priority, userId);
            return ResponseEntity.ok(updatedTask);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.err.println("Error updating priority: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // ═══════════════════════════════════════════════════════════
    // GET TASKS BY TEAM
    // ═══════════════════════════════════════════════════════════
    @GetMapping("/team/{teamId}")
    public ResponseEntity<List<Task>> getTasksByTeam(@PathVariable String teamId) {
        try {
            // Note: In production, use query state store or dedicated search service
            // For now, this is a simplified implementation
            List<Task> tasks = new ArrayList<>();

            // This would typically query a database or use Dapr Query API
            // Placeholder for demonstration
            System.out.println("Fetching tasks for team: " + teamId);

            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            System.err.println("Error getting team tasks: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // ═══════════════════════════════════════════════════════════
    // GET TASKS BY ASSIGNEE
    // ═══════════════════════════════════════════════════════════
    @GetMapping("/assigned/{userId}")
    public ResponseEntity<List<Task>> getTasksByAssignee(@PathVariable String userId) {
        try {
            // Placeholder - would query state store or database
            List<Task> tasks = new ArrayList<>();
            System.out.println("Fetching tasks for user: " + userId);
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            System.err.println("Error getting user tasks: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // ═══════════════════════════════════════════════════════════
    // GET TASKS BY STATUS
    // ═══════════════════════════════════════════════════════════
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Task>> getTasksByStatus(@PathVariable TaskStatus status) {
        try {
            // Placeholder - would query state store or database
            List<Task> tasks = new ArrayList<>();
            System.out.println("Fetching tasks with status: " + status);
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            System.err.println("Error getting tasks by status: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // ═══════════════════════════════════════════════════════════
    // UPDATE TASK (Full update)
    // ═══════════════════════════════════════════════════════════
    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable String id,
                                           @RequestBody Task updatedTask,
                                           @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        try {
            // Get existing task
            var state = client.getState(STATE_STORE, id, Task.class).block();
            if (state == null || state.getValue() == null) {
                return ResponseEntity.notFound().build();
            }

            Task existingTask = state.getValue();

            // Update fields (preserve ID and creation metadata)
            updatedTask.setId(existingTask.getId());
            updatedTask.setCreatedAt(existingTask.getCreatedAt());
            updatedTask.setCreatedBy(existingTask.getCreatedBy());
            updatedTask.setUpdatedAt(LocalDateTime.now().toString());

            // Save updated task
            client.saveState(STATE_STORE, id, updatedTask).block();

            System.out.println(String.format("📝 Task updated: %s by %s", id, userId));

            return ResponseEntity.ok(updatedTask);

        } catch (Exception e) {
            System.err.println("Error updating task: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // ═══════════════════════════════════════════════════════════
    // DELETE TASK
    // ═══════════════════════════════════════════════════════════
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable String id,
                                           @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        try {
            // Verify task exists
            var state = client.getState(STATE_STORE, id, Task.class).block();
            if (state == null || state.getValue() == null) {
                return ResponseEntity.notFound().build();
            }

            // Delete from state store
            client.deleteState(STATE_STORE, id).block();

            System.out.println(String.format("🗑️  Task deleted: %s by %s", id, userId));

            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            System.err.println("Error deleting task: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}