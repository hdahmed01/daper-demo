package tn.ahmed.taskservice.service;


import io.dapr.client.DaprClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import tn.ahmed.taskservice.entities.Priority;
import tn.ahmed.taskservice.entities.Task;
import tn.ahmed.taskservice.entities.TaskStatus;
import tn.ahmed.taskservice.repo.TaskRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TaskService {

    @Autowired
    private TaskRepository repository;

    @Autowired
    private TaskCacheService cacheService;

    @Autowired
    private DaprClient daprClient;

    private final String PUBSUB_NAME = "pubsub";

    private String getCurrentUserId() {
        JwtAuthenticationToken authentication =
                (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new IllegalStateException("No authenticated user found");
        }
        return authentication.getToken().getSubject(); // "sub" claim = Keycloak user ID
    }

    // CREATE TASK
    public Task createTask(Task task) {
        // Set metadata
        String userId = getCurrentUserId();
        task.setId(UUID.randomUUID().toString());
        task.setCreatedBy(userId);
        task.setCreatedAt(LocalDateTime.now().toString());
        task.setUpdatedAt(LocalDateTime.now().toString());

        if (task.getStatus() == null) {
            task.setStatus(TaskStatus.OPEN);
        }
        if (task.getPriority() == null) {
            task.setPriority(Priority.MEDIUM);
        }

        // Save to MongoDB
        Task savedTask = repository.save(task);

        // Cache it
        cacheService.cacheTask(savedTask);

        // Publish event
        try {
            daprClient.publishEvent(PUBSUB_NAME, "task-created", savedTask).block();
        } catch (Exception e) {
            System.err.println("Failed to publish event: " + e.getMessage());
        }

        System.out.println("✅ Task created: " + savedTask.getId());
        return savedTask;
    }

    // GET TASK (with cache)
    public Task getTask(String id) {
        // Try cache first
        Task cached = cacheService.getCachedTask(id);
        if (cached != null) {
            return cached;
        }

        // Cache miss - query MongoDB
        Task task = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));

        // Update cache
        cacheService.cacheTask(task);

        return task;
    }

    // UPDATE TASK
    public Task updateTask(String id, Task updates ) {
        Task task = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));
        String userId = getCurrentUserId();

        // Update fields
        task.setTitle(updates.getTitle());
        task.setDescription(updates.getDescription());
        task.setAssignedTo(updates.getAssignedTo());
        task.setTeamId(updates.getTeamId());
        task.setStatus(updates.getStatus());
        task.setPriority(updates.getPriority());
        task.setEstimatedHours(updates.getEstimatedHours());
        task.setActualHours(updates.getActualHours());
        task.setDueDate(updates.getDueDate());
        task.setTags(updates.getTags());
        task.setUpdatedAt(LocalDateTime.now().toString());

        // Save to MongoDB
        task = repository.save(task);

        // Invalidate cache
        cacheService.invalidateTask(id);

        System.out.println("📝 Task updated: " + id);
        return task;
    }

    // DELETE TASK
    public void deleteTask(String id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Task not found: " + id);
        }

        // Delete from MongoDB
        repository.deleteById(id);

        // Invalidate cache
        cacheService.invalidateTask(id);

        System.out.println("🗑️  Task deleted: " + id);
    }

    // QUERY METHODS
    public List<Task> getTasksByStatus(TaskStatus status) {
        return repository.findByStatus(status);
    }

    public List<Task> getTasksByAssignee(String userId) {
        return repository.findByAssignedTo(userId);
    }

    public List<Task> getTasksByTeam(String teamId) {
        return repository.findByTeamId(teamId);
    }

    public List<Task> getAllTasks() {
        return repository.findAll();
    }

    public long countTasksByStatus(TaskStatus status) {
        return repository.countByStatus(status);
    }
}