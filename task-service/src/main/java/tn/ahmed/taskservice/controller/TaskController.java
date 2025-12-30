package tn.ahmed.taskservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.ahmed.taskservice.service.TaskWorkflowService;
import tn.ahmed.taskservice.entities.Priority;
import tn.ahmed.taskservice.entities.Task;
import tn.ahmed.taskservice.entities.TaskStatus;
import tn.ahmed.taskservice.service.TaskService;

import java.util.List;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskWorkflowService workflowService;

    // CREATE
    @PostMapping
    public ResponseEntity<Task> createTask(
            @RequestBody Task task ) {
        try {
            Task created = taskService.createTask(task);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // GET BY ID
    @GetMapping("/{id}")
    public ResponseEntity<Task> getTask(@PathVariable String id) {
        try {
            Task task = taskService.getTask(id);
            return ResponseEntity.ok(task);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // GET ALL
    @GetMapping
    public ResponseEntity<List<Task>> getAllTasks() {
        try {
            List<Task> tasks = taskService.getAllTasks();
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(
            @PathVariable String id,
            @RequestBody Task task) {
        try {
            Task updated = taskService.updateTask(id, task);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        try {
            taskService.deleteTask(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // GET BY STATUS
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Task>> getTasksByStatus(@PathVariable TaskStatus status) {
        try {
            List<Task> tasks = taskService.getTasksByStatus(status);
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // GET BY USER
    @GetMapping("/assigned/{userId}")
    public ResponseEntity<List<Task>> getTasksByAssignee(@PathVariable String userId) {
        try {
            List<Task> tasks = taskService.getTasksByAssignee(userId);
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // GET BY TEAM
    @GetMapping("/team/{teamId}")
    public ResponseEntity<List<Task>> getTasksByTeam(@PathVariable String teamId) {
        try {
            List<Task> tasks = taskService.getTasksByTeam(teamId);
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // WORKFLOW - UPDATE STATUS
    @PatchMapping("/{id}/status")
    public ResponseEntity<Task> updateStatus(
            @PathVariable String id,
            @RequestParam TaskStatus status) {
        try {
            Task updated = workflowService.transitionStatus(id, status);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // WORKFLOW - ASSIGN
    @PatchMapping("/{id}/assign")
    public ResponseEntity<Task> assignTask(
            @PathVariable String id,
            @RequestParam String assignedTo,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        try {
            Task updated = workflowService.assignTask(id, assignedTo, userId);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // WORKFLOW - UPDATE PRIORITY
    @PatchMapping("/{id}/priority")
    public ResponseEntity<Task> updatePriority(
            @PathVariable String id,
            @RequestParam Priority priority) throws Exception {
        try {
            Task updated = workflowService.updatePriority(id, priority);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}