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

    // ═══════════════════════════════════════════════════════════
    // TASK CREATED EVENT (existing + enhancements)
    // ═══════════════════════════════════════════════════════════
    @PostMapping(path = "/task-created")
    public void onTaskCreated(@RequestBody CloudEvent event) {
        System.out.println("📊 Analytics: Received task-created event");

        try {
            Map<String, Object> data = (Map<String, Object>) event.getData();
            String taskId = data.getOrDefault("id", "unknown").toString();
            String title = data.getOrDefault("title", "untitled").toString();
            String priority = data.getOrDefault("priority", "MEDIUM").toString();
            String teamId = data.getOrDefault("teamId", "unassigned").toString();
            String assignedTo = data.getOrDefault("assignedTo", "unassigned").toString();
            LocalDateTime now = LocalDateTime.now();

            TaskStats stats = getStats();

            // Basic counters
            stats.setTotalTasks(stats.getTotalTasks() + 1);
            stats.setLastUpdated(now.toString());

            // NEW: Track by priority
            updatePriorityStats(stats, priority);

            // NEW: Track by team
            updateTeamStats(stats, teamId);

            // NEW: Track by assignee
            updateAssigneeStats(stats, assignedTo);

            // Existing: Time-based analytics
            updateTimeBasedStats(stats, now);

            // Existing: Word frequency
            updateWordFrequency(stats, title);

            // Existing: Activity tracking
            updateActivityTracking(stats, now);

            // Existing: Streaks
            updateStreaks(stats, now);

            // Existing: Recent tasks
            updateRecentTasks(stats, taskId, title, now);

            // Existing: Performance metrics
            updatePerformanceMetrics(stats);

            // Existing: Top titles
            updateTopTitles(stats);

            // Save
            client.saveState(STATE_STORE, STATS_KEY, stats).block();

            System.out.println("✅ Analytics updated (task created)");

        } catch (Exception e) {
            System.err.println("❌ Error processing analytics: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════════════════════════════════
    // TASK STATUS CHANGED EVENT (NEW)
    // ═══════════════════════════════════════════════════════════
    @PostMapping(path = "/task-status-changed")
    public void onTaskStatusChanged(@RequestBody CloudEvent event) {
        System.out.println("📊 Analytics: Received task-status-changed event");

        try {
            Map<String, Object> data = (Map<String, Object>) event.getData();
            String taskId = data.getOrDefault("taskId", "unknown").toString();
            String oldStatus = data.getOrDefault("oldStatus", "OPEN").toString();
            String newStatus = data.getOrDefault("newStatus", "OPEN").toString();
            LocalDateTime now = LocalDateTime.now();

            TaskStats stats = getStats();

            // Track status transitions
            updateStatusTransitions(stats, oldStatus, newStatus);

            // Track completion rate
            if ("COMPLETED".equals(newStatus)) {
                stats.setCompletedTasks(stats.getCompletedTasks() + 1);

                // Calculate completion rate
                double completionRate = (double) stats.getCompletedTasks() / stats.getTotalTasks() * 100;
                stats.setCompletionRate(completionRate);
            }

            // Track cycle time (OPEN → COMPLETED)
            if ("COMPLETED".equals(newStatus)) {
                updateCycleTime(stats, taskId);
            }

            stats.setLastUpdated(now.toString());
            client.saveState(STATE_STORE, STATS_KEY, stats).block();

            System.out.println("✅ Analytics updated (status changed)");

        } catch (Exception e) {
            System.err.println("❌ Error processing status change: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════════════════════════════════
    // TASK ASSIGNED EVENT (NEW)
    // ═══════════════════════════════════════════════════════════
    @PostMapping(path = "/task-assigned")
    public void onTaskAssigned(@RequestBody CloudEvent event) {
        System.out.println("📊 Analytics: Received task-assigned event");

        try {
            Map<String, Object> data = (Map<String, Object>) event.getData();
            String taskId = data.getOrDefault("taskId", "unknown").toString();
            String newAssignee = data.getOrDefault("newAssignee", "unassigned").toString();
            LocalDateTime now = LocalDateTime.now();

            TaskStats stats = getStats();

            // Update assignee stats
            updateAssigneeStats(stats, newAssignee);

            // Track assignment changes
            stats.setTotalAssignments(stats.getTotalAssignments() + 1);

            stats.setLastUpdated(now.toString());
            client.saveState(STATE_STORE, STATS_KEY, stats).block();

            System.out.println("✅ Analytics updated (task assigned)");

        } catch (Exception e) {
            System.err.println("❌ Error processing assignment: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════════════════════════════════
    // NEW HELPER METHODS
    // ═══════════════════════════════════════════════════════════

    private void updatePriorityStats(TaskStats stats, String priority) {
        Map<String, Integer> priorityMap = stats.getTasksByPriority();
        if (priorityMap == null) priorityMap = new HashMap<>();
        priorityMap.put(priority, priorityMap.getOrDefault(priority, 0) + 1);
        stats.setTasksByPriority(priorityMap);
    }

    private void updateTeamStats(TaskStats stats, String teamId) {
        Map<String, Integer> teamMap = stats.getTasksByTeam();
        if (teamMap == null) teamMap = new HashMap<>();
        teamMap.put(teamId, teamMap.getOrDefault(teamId, 0) + 1);
        stats.setTasksByTeam(teamMap);
    }

    private void updateAssigneeStats(TaskStats stats, String assignee) {
        Map<String, Integer> assigneeMap = stats.getTasksByAssignee();
        if (assigneeMap == null) assigneeMap = new HashMap<>();
        assigneeMap.put(assignee, assigneeMap.getOrDefault(assignee, 0) + 1);
        stats.setTasksByAssignee(assigneeMap);
    }

    private void updateStatusTransitions(TaskStats stats, String oldStatus, String newStatus) {
        Map<String, Integer> transitionsMap = stats.getStatusTransitions();
        if (transitionsMap == null) transitionsMap = new HashMap<>();

        String transition = oldStatus + " → " + newStatus;
        transitionsMap.put(transition, transitionsMap.getOrDefault(transition, 0) + 1);
        stats.setStatusTransitions(transitionsMap);
    }

    private void updateCycleTime(TaskStats stats, String taskId) {
        // In production, would calculate actual time from task creation to completion
        // For now, just track that we completed a task
        stats.setCompletedTaskIds(
                stats.getCompletedTaskIds() == null ?
                        new ArrayList<>(List.of(taskId)) :
                        new ArrayList<>(stats.getCompletedTaskIds())
        );
        if (!stats.getCompletedTaskIds().contains(taskId)) {
            stats.getCompletedTaskIds().add(taskId);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // EXISTING HELPER METHODS (from original)
    // ═══════════════════════════════════════════════════════════

    private void updateTimeBasedStats(TaskStats stats, LocalDateTime now) {
        String hour = String.valueOf(now.getHour());
        stats.getTasksByHour().merge(hour, 1, Integer::sum);

        String dayOfWeek = now.getDayOfWeek().toString();
        stats.getTasksByDayOfWeek().merge(dayOfWeek, 1, Integer::sum);

        String month = now.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        stats.getTasksByMonth().merge(month, 1, Integer::sum);
    }

    private void updateWordFrequency(TaskStats stats, String title) {
        String[] words = title.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .split("\\s+");

        for (String word : words) {
            if (word.length() > 2) {
                stats.getTitleWordFrequency().merge(word, 1, Integer::sum);
            }
        }
    }

    private void updateActivityTracking(TaskStats stats, LocalDateTime now) {
        LocalDate today = now.toLocalDate();
        String todayStr = today.toString();

        if (stats.getLastCountDate() == null) {
            stats.setLastCountDate(todayStr);
            stats.setTasksToday(1);
            stats.setTasksThisWeek(1);
            stats.setTasksThisMonth(1);
            return;
        }

        LocalDate lastCountDate = LocalDate.parse(stats.getLastCountDate());

        if (!todayStr.equals(stats.getLastCountDate())) {
            stats.setTasksToday(1);
            stats.setLastCountDate(todayStr);

            WeekFields weekFields = WeekFields.of(Locale.getDefault());
            int currentWeek = today.get(weekFields.weekOfWeekBasedYear());
            int lastWeek = lastCountDate.get(weekFields.weekOfWeekBasedYear());

            if (currentWeek != lastWeek || today.getYear() != lastCountDate.getYear()) {
                stats.setTasksThisWeek(1);
            } else {
                stats.setTasksThisWeek(stats.getTasksThisWeek() + 1);
            }

            if (today.getMonth() != lastCountDate.getMonth() || today.getYear() != lastCountDate.getYear()) {
                stats.setTasksThisMonth(1);
            } else {
                stats.setTasksThisMonth(stats.getTasksThisMonth() + 1);
            }
        } else {
            stats.setTasksToday(stats.getTasksToday() + 1);
            stats.setTasksThisWeek(stats.getTasksThisWeek() + 1);
            stats.setTasksThisMonth(stats.getTasksThisMonth() + 1);
        }
    }

    private void updateStreaks(TaskStats stats, LocalDateTime now) {
        String today = now.toLocalDate().toString();
        String lastDate = stats.getLastTaskDate();

        if (lastDate == null) {
            stats.setCurrentStreak(1);
            stats.setLongestStreak(1);
            stats.setTotalDaysActive(1);
        } else if (!lastDate.equals(today)) {
            LocalDate lastDateTime = LocalDate.parse(lastDate);
            long daysBetween = ChronoUnit.DAYS.between(lastDateTime, now.toLocalDate());

            if (daysBetween == 1) {
                stats.setCurrentStreak(stats.getCurrentStreak() + 1);
                stats.setTotalDaysActive(stats.getTotalDaysActive() + 1);
                if (stats.getCurrentStreak() > stats.getLongestStreak()) {
                    stats.setLongestStreak(stats.getCurrentStreak());
                }
            } else if (daysBetween > 1) {
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

        recentTasks.add(0, TaskStats.RecentTask.builder()
                .id(taskId)
                .title(title)
                .timestamp(now.toString())
                .build());

        if (recentTasks.size() > 10) {
            recentTasks = recentTasks.subList(0, 10);
        }

        stats.setRecentTasks(recentTasks);
    }

    private void updatePerformanceMetrics(TaskStats stats) {
        if (!stats.getTasksByHour().isEmpty()) {
            var busiestHourEntry = stats.getTasksByHour().entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .orElse(null);

            if (busiestHourEntry != null) {
                stats.setBusiestHour(busiestHourEntry.getKey() + ":00");
                stats.setPeakTasksInOneHour(busiestHourEntry.getValue());
            }
        }

        if (!stats.getTasksByDayOfWeek().isEmpty()) {
            var busiestDayEntry = stats.getTasksByDayOfWeek().entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .orElse(null);

            if (busiestDayEntry != null) {
                stats.setBusiestDay(busiestDayEntry.getKey());
            }
        }

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

    // ═══════════════════════════════════════════════════════════
    // REST API ENDPOINTS (existing + new)
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/stats")
    public TaskStats getStatistics() {
        return getStats();
    }

    @GetMapping("/stats/team/{teamId}")
    public Map<String, Object> getTeamStats(@PathVariable String teamId) {
        TaskStats stats = getStats();
        Map<String, Object> teamStats = new HashMap<>();

        teamStats.put("teamId", teamId);
        teamStats.put("totalTasks", stats.getTasksByTeam().getOrDefault(teamId, 0));
        teamStats.put("completionRate", stats.getCompletionRate());

        return teamStats;
    }

    @GetMapping("/stats/assignee/{assignee}")
    public Map<String, Object> getAssigneeStats(@PathVariable String assignee) {
        TaskStats stats = getStats();
        Map<String, Object> assigneeStats = new HashMap<>();

        assigneeStats.put("assignee", assignee);
        assigneeStats.put("totalTasks", stats.getTasksByAssignee().getOrDefault(assignee, 0));

        return assigneeStats;
    }

    @GetMapping("/stats/summary")
    public Map<String, Object> getStatsSummary() {
        TaskStats stats = getStats();
        Map<String, Object> summary = new HashMap<>();

        summary.put("totalTasks", stats.getTotalTasks());
        summary.put("completedTasks", stats.getCompletedTasks());
        summary.put("completionRate", String.format("%.1f%%", stats.getCompletionRate()));
        summary.put("tasksToday", stats.getTasksToday());
        summary.put("currentStreak", stats.getCurrentStreak());
        summary.put("busiestHour", stats.getBusiestHour());

        return summary;
    }

    @GetMapping("/stats/trends")
    public Map<String, Object> getTrends() {
        TaskStats stats = getStats();
        Map<String, Object> trends = new HashMap<>();

        trends.put("tasksByHour", stats.getTasksByHour());
        trends.put("tasksByDayOfWeek", stats.getTasksByDayOfWeek());
        trends.put("tasksByMonth", stats.getTasksByMonth());
        trends.put("tasksByPriority", stats.getTasksByPriority());
        trends.put("tasksByTeam", stats.getTasksByTeam());
        trends.put("statusTransitions", stats.getStatusTransitions());
        trends.put("wordCloud", stats.getTitleWordFrequency());

        return trends;
    }

    @GetMapping("/stats/recent")
    public List<TaskStats.RecentTask> getRecentTasks() {
        return getStats().getRecentTasks();
    }

    private TaskStats getStats() {
        var state = client.getState(STATE_STORE, STATS_KEY, TaskStats.class).block();

        if (state == null || state.getValue() == null) {
            String today = LocalDate.now().toString();
            return TaskStats.builder()
                    .totalTasks(0)
                    .completedTasks(0)
                    .completionRate(0.0)
                    .tasksToday(0)
                    .tasksThisWeek(0)
                    .lastUpdated(LocalDateTime.now().toString())
                    .lastCountDate(today)
                    .tasksByTitle(new HashMap<>())
                    .tasksByHour(new HashMap<>())
                    .tasksByDayOfWeek(new HashMap<>())
                    .tasksByMonth(new HashMap<>())
                    .tasksByPriority(new HashMap<>())
                    .tasksByTeam(new HashMap<>())
                    .tasksByAssignee(new HashMap<>())
                    .statusTransitions(new HashMap<>())
                    .titleWordFrequency(new HashMap<>())
                    .recentTasks(new ArrayList<>())
                    .topTitles(new ArrayList<>())
                    .completedTaskIds(new ArrayList<>())
                    .totalAssignments(0)
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