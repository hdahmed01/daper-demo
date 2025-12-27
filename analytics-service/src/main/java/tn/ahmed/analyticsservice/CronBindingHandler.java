package tn.ahmed.analyticsservice;

import io.dapr.client.DaprClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;


@RestController
public class CronBindingHandler {

    @Autowired
    private DaprClient client;

    private final String STATE_STORE = "statestore";
    private final String STATS_KEY = "global-task-stats";

    /**
     * ═══════════════════════════════════════════════════════════════
     * CRON ENDPOINT - Automatically called by Dapr Cron Binding
     * ═══════════════════════════════════════════════════════════════
     *
     * Configuration in cron-binding.yaml:
     *
     * apiVersion: dapr.io/v1alpha1
     * kind: Component
     * metadata:
     *   name: daily-reset-cron
     * spec:
     *   type: bindings.cron
     *   metadata:
     *   - name: schedule
     *     value: "0 0 * * *"  # Midnight daily
     *
     * The endpoint name must match the component name with a suffix:
     * Component name: "daily-reset-cron"
     * Endpoint: POST /daily-reset-cron
     *
     * Dapr automatically POSTs to this endpoint when the schedule triggers.
     */
    @PostMapping("/daily-reset-cron")
    public void handleDailyReset() {
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("🔄 CRON JOB TRIGGERED!");
        System.out.println("   Time: " + LocalDateTime.now());
        System.out.println("   Action: Resetting daily statistics");
        System.out.println("═══════════════════════════════════════════════════════");

        try {
            // ──────────────────────────────────────────────────────
            // STEP 1: Get current stats
            // ──────────────────────────────────────────────────────
            var state = client.getState(STATE_STORE, STATS_KEY, TaskStats.class).block();

            if (state != null && state.getValue() != null) {
                TaskStats stats = state.getValue();

                System.out.println("   Before reset:");
                System.out.println("     - Total Tasks: " + stats.getTotalTasks());
                System.out.println("     - Tasks Today: " + stats.getTasksToday());

                // ──────────────────────────────────────────────────────
                // STEP 2: Reset daily counter (keep total and distribution)
                // ──────────────────────────────────────────────────────
                stats.setTasksToday(0);
                stats.setLastUpdated(LocalDateTime.now().toString());

                // ──────────────────────────────────────────────────────
                // STEP 3: Save updated stats
                // ──────────────────────────────────────────────────────
                client.saveState(STATE_STORE, STATS_KEY, stats).block();

                System.out.println("   After reset:");
                System.out.println("     - Total Tasks: " + stats.getTotalTasks() + " (unchanged)");
                System.out.println("     - Tasks Today: " + stats.getTasksToday() + " (reset to 0)");
                System.out.println("✅ Daily stats reset successfully!");

            } else {
                System.out.println("⚠️  No stats found in State Store - nothing to reset");
            }

        } catch (Exception e) {
            System.err.println("❌ Error in cron job: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("═══════════════════════════════════════════════════════");
    }

    /**
     * Test endpoint to manually trigger the cron job
     * Usage: curl -X POST http://localhost:8083/trigger-cron-test
     */
    @PostMapping("/trigger-cron-test")
    public String testCronTrigger() {
        System.out.println("🧪 Manual cron test triggered");
        handleDailyReset();
        return "Cron job triggered manually for testing";
    }
}