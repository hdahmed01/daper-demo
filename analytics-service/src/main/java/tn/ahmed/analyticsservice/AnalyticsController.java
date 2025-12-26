package tn.ahmed.analyticsservice;


import com.fasterxml.jackson.databind.JsonNode;
import io.dapr.client.DaprClient;
import io.dapr.client.domain.CloudEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Analytics Controller - Main business logic
 *
 * Endpoints:
 * 1. POST /task-created - Receives events from Dapr Pub/Sub (subscriber)
 * 2. GET /stats - Returns current statistics (REST API)
 * 3. POST /reset-daily - Manually resets daily statistics
 *
 * Flow:
 * Task Service → Dapr Pub/Sub → Dapr Sidecar → POST /task-created
 *                                                      ↓
 *                                                Read stats from State Store
 *                                                Update counters
 *                                                Save back to State Store
 */
@RestController
public class AnalyticsController {

    @Autowired
    private DaprClient client;

    // Dapr component names (defined in YAML files)
    private final String STATE_STORE = "statestore";
    private final String STATS_KEY = "global-task-stats";

    /**
     * ═══════════════════════════════════════════════════════════════
     * SUBSCRIBER ENDPOINT - Receives task-created events
     * ═══════════════════════════════════════════════════════════════
     *
     * This endpoint is called automatically by Dapr when a message
     * is published to the "task-created" topic.
     *
     * Configuration: components/analytics-subscription.yaml
     *
     * CloudEvent structure:
     * {
     *   "specversion": "1.0",
     *   "type": "com.dapr.event.sent",
     *   "source": "task-service",
     *   "id": "uuid",
     *   "time": "2024-12-26T10:30:00Z",
     *   "datacontenttype": "application/json",
     *   "data": {
     *     "id": "task-123",
     *     "title": "Buy groceries",
     *     "description": "Milk, eggs, bread"
     *   }
     * }
     */
    @PostMapping(path = "/task-created")
    public void onTaskCreated(@RequestBody CloudEvent event) {
        System.out.println("📊 Analytics: Received task-created event");
        System.out.println("   Event ID: " + event.getId());
        System.out.println("   Source: " + event.getSource());

        try {
            // ──────────────────────────────────────────────────────
            // STEP 1: Extract task data from CloudEvent
            // ──────────────────────────────────────────────────────
            JsonNode data = (JsonNode) event.getData();
            String taskId = data.has("id") ? data.get("id").asText() : "unknown";
            String title = data.has("title") ? data.get("title").asText() : "untitled";

            System.out.println("   Task ID: " + taskId);
            System.out.println("   Title: " + title);

            // ──────────────────────────────────────────────────────
            // STEP 2: Get current statistics from State Store
            // ──────────────────────────────────────────────────────
            TaskStats stats = getStats();

            // ──────────────────────────────────────────────────────
            // STEP 3: Update statistics
            // ──────────────────────────────────────────────────────
            stats.setTotalTasks(stats.getTotalTasks() + 1);
            stats.setTasksToday(stats.getTasksToday() + 1);
            stats.setLastUpdated(LocalDateTime.now().toString());

            // ──────────────────────────────────────────────────────
            // STEP 4: Update task distribution by title
            // ──────────────────────────────────────────────────────
            Map<String, Integer> tasksByTitle = stats.getTasksByTitle();
            if (tasksByTitle == null) {
                tasksByTitle = new HashMap<>();
            }
            tasksByTitle.put(title, tasksByTitle.getOrDefault(title, 0) + 1);
            stats.setTasksByTitle(tasksByTitle);

            // ──────────────────────────────────────────────────────
            // STEP 5: Save updated statistics to State Store
            // ──────────────────────────────────────────────────────
            client.saveState(STATE_STORE, STATS_KEY, stats).block();

            System.out.println("✅ Analytics updated successfully!");
            System.out.println("   Total Tasks: " + stats.getTotalTasks());
            System.out.println("   Tasks Today: " + stats.getTasksToday());
            System.out.println("   Distribution: " + tasksByTitle);

        } catch (Exception e) {
            System.err.println("❌ Error processing analytics: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * ═══════════════════════════════════════════════════════════════
     * REST API - Get current statistics
     * ═══════════════════════════════════════════════════════════════
     *
     * Usage: curl http://localhost:8083/stats
     *
     * Response example:
     * {
     *   "totalTasks": 150,
     *   "tasksToday": 12,
     *   "lastUpdated": "2024-12-26T15:30:45.123",
     *   "tasksByTitle": {
     *     "Buy groceries": 35,
     *     "Write report": 22
     *   }
     * }
     */
    @GetMapping("/stats")
    public TaskStats getStatistics() {
        System.out.println("📈 GET /stats - Fetching current statistics");
        TaskStats stats = getStats();
        System.out.println("   Total: " + stats.getTotalTasks() +
                ", Today: " + stats.getTasksToday());
        return stats;
    }

    /**
     * ═══════════════════════════════════════════════════════════════
     * MANUAL RESET - Reset daily statistics
     * ═══════════════════════════════════════════════════════════════
     *
     * Usage: curl -X POST http://localhost:8083/reset-daily
     *
     * This endpoint can be:
     * - Called manually for testing
     * - Called automatically by Cron binding at midnight
     * - Called by an external scheduler
     */
    @PostMapping("/reset-daily")
    public String resetDailyStats() {
        System.out.println("🔄 POST /reset-daily - Resetting daily statistics");

        try {
            TaskStats stats = getStats();

            // Reset only daily counter, keep total and distribution
            stats.setTasksToday(0);
            stats.setLastUpdated(LocalDateTime.now().toString());

            client.saveState(STATE_STORE, STATS_KEY, stats).block();

            System.out.println("✅ Daily stats reset successfully at: " + LocalDateTime.now());
            return "Daily stats reset successfully";

        } catch (Exception e) {
            System.err.println("❌ Error resetting stats: " + e.getMessage());
            return "Error resetting stats: " + e.getMessage();
        }
    }

    /**
     * ═══════════════════════════════════════════════════════════════
     * HELPER METHOD - Get stats from State Store
     * ═══════════════════════════════════════════════════════════════
     *
     * Retrieves statistics from Dapr State Store (Redis).
     * If stats don't exist (first run), returns default initialized values.
     *
     * State Store Operation:
     * GET http://localhost:3502/v1.0/state/statestore/global-task-stats
     */
    private TaskStats getStats() {
        var state = client.getState(STATE_STORE, STATS_KEY, TaskStats.class).block();

        if (state == null || state.getValue() == null) {
            System.out.println("   No existing stats found - initializing defaults");

            // Initialize with default values (first run)
            return TaskStats.builder()
                    .totalTasks(0)
                    .tasksToday(0)
                    .lastUpdated(LocalDateTime.now().toString())
                    .tasksByTitle(new HashMap<>())
                    .build();
        }

        System.out.println("   Stats retrieved from State Store");
        return state.getValue();
    }
}
