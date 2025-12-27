package tn.ahmed.analyticsservice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Enhanced Task Statistics Model
 * Stores comprehensive analytics about task creation and patterns
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskStats {

    // ═══════════════════════════════════════════════════════════
    // BASIC COUNTERS
    // ═══════════════════════════════════════════════════════════
    private int totalTasks;
    private int tasksToday;
    private String lastUpdated;

    // ═══════════════════════════════════════════════════════════
    // TITLE ANALYSIS
    // ═══════════════════════════════════════════════════════════
    private Map<String, Integer> tasksByTitle;

    // ═══════════════════════════════════════════════════════════
    // TIME-BASED ANALYTICS
    // ═══════════════════════════════════════════════════════════
    @Builder.Default
    private Map<String, Integer> tasksByHour = new HashMap<>();  // "14" -> 25 tasks

    @Builder.Default
    private Map<String, Integer> tasksByDayOfWeek = new HashMap<>();  // "Monday" -> 120 tasks

    @Builder.Default
    private Map<String, Integer> tasksByMonth = new HashMap<>();  // "2024-12" -> 450 tasks

    // ═══════════════════════════════════════════════════════════
    // PERFORMANCE METRICS
    // ═══════════════════════════════════════════════════════════
    @Builder.Default
    private double averageTasksPerDay = 0.0;

    @Builder.Default
    private int peakTasksInOneHour = 0;

    @Builder.Default
    private String busiestHour = "N/A";  // "14:00" (2 PM)

    @Builder.Default
    private String busiestDay = "N/A";  // "Monday"

    // ═══════════════════════════════════════════════════════════
    // ACTIVITY TRACKING
    // ═══════════════════════════════════════════════════════════
    @Builder.Default
    private int tasksThisWeek = 0;

    @Builder.Default
    private int tasksThisMonth = 0;

    @Builder.Default
    private int tasksLastHour = 0;

    // Internal tracking fields
    private String lastCountDate;  // "2024-12-26" - for daily/weekly/monthly resets
    private String lastHourTimestamp;  // For hourly counter reset

    // ═══════════════════════════════════════════════════════════
    // TRENDING & INSIGHTS
    // ═══════════════════════════════════════════════════════════
    @Builder.Default
    private List<String> topTitles = new ArrayList<>();  // Top 5 most common titles

    @Builder.Default
    private Map<String, Integer> titleWordFrequency = new HashMap<>();  // Word cloud data

    // ═══════════════════════════════════════════════════════════
    // STREAKS & MILESTONES
    // ═══════════════════════════════════════════════════════════
    @Builder.Default
    private int currentStreak = 0;  // Consecutive days with tasks

    @Builder.Default
    private int longestStreak = 0;

    @Builder.Default
    private String lastTaskDate = null;  // "2024-12-26"

    // ═══════════════════════════════════════════════════════════
    // VELOCITY METRICS
    // ═══════════════════════════════════════════════════════════
    @Builder.Default
    private double tasksPerHourAverage = 0.0;

    @Builder.Default
    private int totalDaysActive = 0;  // Days with at least 1 task

    // ═══════════════════════════════════════════════════════════
    // RECENT ACTIVITY
    // ═══════════════════════════════════════════════════════════
    @Builder.Default
    private List<RecentTask> recentTasks = new ArrayList<>();  // Last 10 tasks

    // Helper class for recent tasks
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentTask {
        private String id;
        private String title;
        private String timestamp;
    }
}