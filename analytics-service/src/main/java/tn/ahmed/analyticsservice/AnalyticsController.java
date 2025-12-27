package tn.ahmed.analyticsservice;

import io.dapr.client.DaprClient;
import io.dapr.client.domain.CloudEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class AnalyticsController {

    @Autowired
    private DaprClient client;

    private final String STATE_STORE = "statestore";
    private final String STATS_KEY = "global-task-stats";

    @PostMapping(path = "/task-created")
    public void onTaskCreated(@RequestBody CloudEvent event) {
        System.out.println("📊 Analytics: Received task-created event");
        System.out.println("   Event ID: " + event.getId());
        System.out.println("   Source: " + event.getSource());

        try {
            // Extract task data
            Map<String, Object> data = (Map<String, Object>) event.getData();
            String taskId = data.getOrDefault("id", "unknown").toString();
            String title = data.getOrDefault("title", "untitled").toString();
            LocalDateTime now = LocalDateTime.now();

            System.out.println("   Task ID: " + taskId);
            System.out.println("   Title: " + title);

            // Get current statistics
            TaskStats stats = getStats();

            // ═══════════════════════════════════════════════════════════
            // UPDATE ALL STATISTICS
            // ═══════════════════════════════════════════════════════════

            // Basic counters
            stats.setTotalTasks(stats.getTotalTasks() + 1);
            stats.setLastUpdated(now.toString());

            // Title distribution
            Map<String, Integer> tasksByTitle = stats.getTasksByTitle();
            if (tasksByTitle == null) tasksByTitle = new HashMap<>();
            tasksByTitle.put(title, tasksByTitle.getOrDefault(title, 0) + 1);
            stats.setTasksByTitle(tasksByTitle);

            // Time-based analytics
            updateTimeBasedStats(stats, now);

            // Word frequency for word cloud
            updateWordFrequency(stats, title);

            // Activity tracking (with proper date checking)
            updateActivityTracking(stats, now);

            // Streaks
            updateStreaks(stats, now);

            // Recent tasks (keep last 10)
            updateRecentTasks(stats, taskId, title, now);

            // Performance metrics
            updatePerformanceMetrics(stats);

            // Top titles (top 5)
            updateTopTitles(stats);

            // Save updated statistics
            client.saveState(STATE_STORE, STATS_KEY, stats).block();

            System.out.println("✅ Analytics updated successfully!");
            System.out.println("   Total: " + stats.getTotalTasks());
            System.out.println("   Today: " + stats.getTasksToday());
            System.out.println("   This Week: " + stats.getTasksThisWeek());
            System.out.println("   Busiest Hour: " + stats.getBusiestHour());
            System.out.println("   Current Streak: " + stats.getCurrentStreak() + " days");

        } catch (Exception e) {
            System.err.println("❌ Error processing analytics: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPER METHODS FOR STATISTICS UPDATES
    // ═══════════════════════════════════════════════════════════════

    private void updateTimeBasedStats(TaskStats stats, LocalDateTime now) {
        // Hour of day (0-23)
        String hour = String.valueOf(now.getHour());
        stats.getTasksByHour().merge(hour, 1, Integer::sum);

        // Day of week
        String dayOfWeek = now.getDayOfWeek().toString();
        stats.getTasksByDayOfWeek().merge(dayOfWeek, 1, Integer::sum);

        // Month
        String month = now.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        stats.getTasksByMonth().merge(month, 1, Integer::sum);
    }

    private void updateWordFrequency(TaskStats stats, String title) {
        // Split title into words and count frequency
        String[] words = title.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .split("\\s+");

        for (String word : words) {
            if (word.length() > 2) { // Ignore very short words
                stats.getTitleWordFrequency().merge(word, 1, Integer::sum);
            }
        }
    }

    private void updateActivityTracking(TaskStats stats, LocalDateTime now) {
        LocalDate today = now.toLocalDate();
        String todayStr = today.toString();

        // Initialize tracking date if null
        if (stats.getLastCountDate() == null) {
            stats.setLastCountDate(todayStr);
            stats.setTasksToday(1);
            stats.setTasksThisWeek(1);
            stats.setTasksThisMonth(1);
            stats.setTasksLastHour(1);
            stats.setLastHourTimestamp(now.toString());
            return;
        }

        LocalDate lastCountDate = LocalDate.parse(stats.getLastCountDate());

        // Check if it's a new day - reset daily counters
        if (!todayStr.equals(stats.getLastCountDate())) {
            stats.setTasksToday(1);
            stats.setLastCountDate(todayStr);

            // Check if it's a new week
            WeekFields weekFields = WeekFields.of(Locale.getDefault());
            int currentWeek = today.get(weekFields.weekOfWeekBasedYear());
            int lastWeek = lastCountDate.get(weekFields.weekOfWeekBasedYear());

            if (currentWeek != lastWeek || today.getYear() != lastCountDate.getYear()) {
                stats.setTasksThisWeek(1);
            } else {
                stats.setTasksThisWeek(stats.getTasksThisWeek() + 1);
            }

            // Check if it's a new month
            if (today.getMonth() != lastCountDate.getMonth() || today.getYear() != lastCountDate.getYear()) {
                stats.setTasksThisMonth(1);
            } else {
                stats.setTasksThisMonth(stats.getTasksThisMonth() + 1);
            }
        } else {
            // Same day - just increment
            stats.setTasksToday(stats.getTasksToday() + 1);
            stats.setTasksThisWeek(stats.getTasksThisWeek() + 1);
            stats.setTasksThisMonth(stats.getTasksThisMonth() + 1);
        }

        // Update last hour counter
        if (stats.getLastHourTimestamp() != null) {
            LocalDateTime lastHourTime = LocalDateTime.parse(stats.getLastHourTimestamp());
            long minutesSinceLastCount = ChronoUnit.MINUTES.between(lastHourTime, now);

            if (minutesSinceLastCount >= 60) {
                stats.setTasksLastHour(1);
                stats.setLastHourTimestamp(now.toString());
            } else {
                stats.setTasksLastHour(stats.getTasksLastHour() + 1);
            }
        } else {
            stats.setTasksLastHour(1);
            stats.setLastHourTimestamp(now.toString());
        }
    }

    private void updateStreaks(TaskStats stats, LocalDateTime now) {
        String today = now.toLocalDate().toString();
        String lastDate = stats.getLastTaskDate();

        if (lastDate == null) {
            // First task ever
            stats.setCurrentStreak(1);
            stats.setLongestStreak(1);
            stats.setTotalDaysActive(1);
        } else if (!lastDate.equals(today)) {
            // Different day
            LocalDate lastDateTime = LocalDate.parse(lastDate);
            long daysBetween = ChronoUnit.DAYS.between(lastDateTime, now.toLocalDate());

            if (daysBetween == 1) {
                // Consecutive day - increment streak
                stats.setCurrentStreak(stats.getCurrentStreak() + 1);
                stats.setTotalDaysActive(stats.getTotalDaysActive() + 1);
                if (stats.getCurrentStreak() > stats.getLongestStreak()) {
                    stats.setLongestStreak(stats.getCurrentStreak());
                }
            } else if (daysBetween > 1) {
                // Streak broken - reset to 1
                stats.setCurrentStreak(1);
                stats.setTotalDaysActive(stats.getTotalDaysActive() + 1);
            }
        }

        stats.setLastTaskDate(today);
    }

    private void updateRecentTasks(TaskStats stats, String taskId, String title, LocalDateTime now) {
        List<TaskStats.RecentTask> recentTasks = stats.getRecentTasks();
        if (recentTasks == null) {
            recentTasks = new ArrayList<>();
        }

        // Add new task
        recentTasks.add(0, TaskStats.RecentTask.builder()
                .id(taskId)
                .title(title)
                .timestamp(now.toString())
                .build());

        // Keep only last 10
        if (recentTasks.size() > 10) {
            recentTasks = recentTasks.subList(0, 10);
        }

        stats.setRecentTasks(recentTasks);
    }

    private void updatePerformanceMetrics(TaskStats stats) {
        // Find busiest hour
        if (!stats.getTasksByHour().isEmpty()) {
            var busiestHourEntry = stats.getTasksByHour().entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .orElse(null);

            if (busiestHourEntry != null) {
                stats.setBusiestHour(busiestHourEntry.getKey() + ":00");
                stats.setPeakTasksInOneHour(busiestHourEntry.getValue());
            }
        }

        // Find busiest day
        if (!stats.getTasksByDayOfWeek().isEmpty()) {
            var busiestDayEntry = stats.getTasksByDayOfWeek().entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .orElse(null);

            if (busiestDayEntry != null) {
                stats.setBusiestDay(busiestDayEntry.getKey());
            }
        }

        // Calculate averages
        if (stats.getTotalDaysActive() > 0) {
            stats.setAverageTasksPerDay(
                    (double) stats.getTotalTasks() / stats.getTotalDaysActive()
            );
        }
    }

    private void updateTopTitles(TaskStats stats) {
        if (stats.getTasksByTitle() == null || stats.getTasksByTitle().isEmpty()) {
            return;
        }

        List<String> topTitles = stats.getTasksByTitle().entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        stats.setTopTitles(topTitles);
    }

    // ═══════════════════════════════════════════════════════════════
    // REST API ENDPOINTS
    // ═══════════════════════════════════════════════════════════════

    @GetMapping("/stats")
    public TaskStats getStatistics() {
        System.out.println("📈 GET /stats - Fetching current statistics");
        return getStats();
    }

    @GetMapping("/stats/summary")
    public Map<String, Object> getStatsSummary() {
        TaskStats stats = getStats();
        Map<String, Object> summary = new HashMap<>();

        summary.put("totalTasks", stats.getTotalTasks());
        summary.put("tasksToday", stats.getTasksToday());
        summary.put("tasksThisWeek", stats.getTasksThisWeek());
        summary.put("currentStreak", stats.getCurrentStreak() + " days");
        summary.put("busiestHour", stats.getBusiestHour());
        summary.put("busiestDay", stats.getBusiestDay());
        summary.put("topTitles", stats.getTopTitles());

        return summary;
    }

    @GetMapping("/stats/trends")
    public Map<String, Object> getTrends() {
        TaskStats stats = getStats();
        Map<String, Object> trends = new HashMap<>();

        trends.put("tasksByHour", stats.getTasksByHour());
        trends.put("tasksByDayOfWeek", stats.getTasksByDayOfWeek());
        trends.put("tasksByMonth", stats.getTasksByMonth());
        trends.put("wordCloud", stats.getTitleWordFrequency());

        return trends;
    }

    @GetMapping("/stats/recent")
    public List<TaskStats.RecentTask> getRecentTasks() {
        return getStats().getRecentTasks();
    }

    @PostMapping("/reset-daily")
    public String resetDailyStats() {
        System.out.println("🔄 POST /reset-daily - Resetting daily statistics");

        try {
            TaskStats stats = getStats();
            stats.setTasksToday(0);
            stats.setTasksLastHour(0);
            stats.setLastUpdated(LocalDateTime.now().toString());

            client.saveState(STATE_STORE, STATS_KEY, stats).block();
            System.out.println("✅ Daily stats reset successfully");
            return "Daily stats reset successfully";

        } catch (Exception e) {
            System.err.println("❌ Error resetting stats: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @PostMapping("/reset-all")
    public String resetAllStats() {
        System.out.println("🔄 POST /reset-all - Resetting ALL statistics");

        try {
            TaskStats freshStats = TaskStats.builder()
                    .totalTasks(0)
                    .tasksToday(0)
                    .tasksThisWeek(0)
                    .tasksThisMonth(0)
                    .tasksLastHour(0)
                    .lastUpdated(LocalDateTime.now().toString())
                    .tasksByTitle(new HashMap<>())
                    .tasksByHour(new HashMap<>())
                    .tasksByDayOfWeek(new HashMap<>())
                    .tasksByMonth(new HashMap<>())
                    .titleWordFrequency(new HashMap<>())
                    .recentTasks(new ArrayList<>())
                    .topTitles(new ArrayList<>())
                    .currentStreak(0)
                    .longestStreak(0)
                    .totalDaysActive(0)
                    .busiestHour("N/A")
                    .busiestDay("N/A")
                    .build();

            client.saveState(STATE_STORE, STATS_KEY, freshStats).block();
            System.out.println("✅ All stats reset successfully");
            return "All statistics reset successfully";

        } catch (Exception e) {
            System.err.println("❌ Error resetting stats: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    private TaskStats getStats() {
        var state = client.getState(STATE_STORE, STATS_KEY, TaskStats.class).block();

        if (state == null || state.getValue() == null) {
            System.out.println("   No existing stats found - initializing defaults");
            String today = LocalDate.now().toString();
            return TaskStats.builder()
                    .totalTasks(0)
                    .tasksToday(0)
                    .tasksThisWeek(0)
                    .tasksThisMonth(0)
                    .tasksLastHour(0)
                    .lastUpdated(LocalDateTime.now().toString())
                    .lastCountDate(today)
                    .lastHourTimestamp(LocalDateTime.now().toString())
                    .tasksByTitle(new HashMap<>())
                    .tasksByHour(new HashMap<>())
                    .tasksByDayOfWeek(new HashMap<>())
                    .tasksByMonth(new HashMap<>())
                    .titleWordFrequency(new HashMap<>())
                    .recentTasks(new ArrayList<>())
                    .topTitles(new ArrayList<>())
                    .currentStreak(0)
                    .longestStreak(0)
                    .totalDaysActive(0)
                    .busiestHour("N/A")
                    .busiestDay("N/A")
                    .build();
        }

        return state.getValue();
    }
}