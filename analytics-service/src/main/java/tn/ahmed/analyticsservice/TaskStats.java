package tn.ahmed.analyticsservice;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TaskStats {

    // ═══════════════════════════════════════════════════════════
    // Basic Counters
    // ═══════════════════════════════════════════════════════════
    @JsonProperty("totalTasks")
    @Builder.Default
    private int totalTasks = 0;

    @JsonProperty("completedTasks")
    @Builder.Default
    private int completedTasks = 0;

    @JsonProperty("completionRate")
    @Builder.Default
    private double completionRate = 0.0;

    @JsonProperty("totalAssignments")
    @Builder.Default
    private int totalAssignments = 0;

    // ═══════════════════════════════════════════════════════════
    // Time-based Tracking
    // ═══════════════════════════════════════════════════════════
    @JsonProperty("tasksToday")
    @Builder.Default
    private int tasksToday = 0;

    @JsonProperty("tasksThisWeek")
    @Builder.Default
    private int tasksThisWeek = 0;

    @JsonProperty("tasksThisMonth")
    @Builder.Default
    private int tasksThisMonth = 0;

    @JsonProperty("lastCountDate")
    private String lastCountDate;  // "2025-01-15"

    @JsonProperty("lastTaskDate")
    private String lastTaskDate;

    @JsonProperty("lastUpdated")
    private String lastUpdated;  // ISO timestamp string

    // ═══════════════════════════════════════════════════════════
    // Streaks & Activity
    // ═══════════════════════════════════════════════════════════
    @JsonProperty("currentStreak")
    @Builder.Default
    private int currentStreak = 0;

    @JsonProperty("longestStreak")
    @Builder.Default
    private int longestStreak = 0;

    @JsonProperty("totalDaysActive")
    @Builder.Default
    private int totalDaysActive = 0;

    // ═══════════════════════════════════════════════════════════
    // Distribution Maps
    // ═══════════════════════════════════════════════════════════
    @JsonProperty("tasksByHour")
    @Builder.Default
    private Map<String, Integer> tasksByHour = new java.util.HashMap<>();

    @JsonProperty("tasksByDayOfWeek")
    @Builder.Default
    private Map<String, Integer> tasksByDayOfWeek = new java.util.HashMap<>();

    @JsonProperty("tasksByMonth")
    @Builder.Default
    private Map<String, Integer> tasksByMonth = new java.util.HashMap<>();

    @JsonProperty("tasksByPriority")
    @Builder.Default
    private Map<String, Integer> tasksByPriority = new java.util.HashMap<>();

    @JsonProperty("tasksByTeam")
    @Builder.Default
    private Map<String, Integer> tasksByTeam = new java.util.HashMap<>();

    @JsonProperty("tasksByAssignee")
    @Builder.Default
    private Map<String, Integer> tasksByAssignee = new java.util.HashMap<>();

    @JsonProperty("tasksByCreator")
    @Builder.Default
    private Map<String, Integer> tasksByCreator = new java.util.HashMap<>();

    @JsonProperty("tasksByTitle")
    @Builder.Default
    private Map<String, Integer> tasksByTitle = new java.util.HashMap<>();

    // ═══════════════════════════════════════════════════════════
    // Workflow Tracking
    // ═══════════════════════════════════════════════════════════
    @JsonProperty("statusTransitions")
    @Builder.Default
    private Map<String, Integer> statusTransitions = new java.util.HashMap<>();

    @JsonProperty("priorityChanges")
    @Builder.Default
    private Map<String, Integer> priorityChanges = new java.util.HashMap<>();

    // ═══════════════════════════════════════════════════════════
    // Word Analytics
    // ═══════════════════════════════════════════════════════════
    @JsonProperty("titleWordFrequency")
    @Builder.Default
    private Map<String, Integer> titleWordFrequency = new java.util.HashMap<>();

    // ═══════════════════════════════════════════════════════════
    // Lists
    // ═══════════════════════════════════════════════════════════
    @JsonProperty("recentTasks")
    @Builder.Default
    private List<RecentTask> recentTasks = new java.util.ArrayList<>();

    @JsonProperty("topTitles")
    @Builder.Default
    private List<String> topTitles = new java.util.ArrayList<>();

    @JsonProperty("completedTaskIds")
    @Builder.Default
    private List<String> completedTaskIds = new java.util.ArrayList<>();

    // ═══════════════════════════════════════════════════════════
    // Performance Metrics
    // ═══════════════════════════════════════════════════════════
    @JsonProperty("busiestHour")
    @Builder.Default
    private String busiestHour = "N/A";

    @JsonProperty("busiestDay")
    @Builder.Default
    private String busiestDay = "N/A";

    @JsonProperty("peakTasksInOneHour")
    @Builder.Default
    private int peakTasksInOneHour = 0;

    @JsonProperty("averageTasksPerDay")
    @Builder.Default
    private double averageTasksPerDay = 0.0;

    // ═══════════════════════════════════════════════════════════
    // Nested Class: RecentTask
    // ═══════════════════════════════════════════════════════════
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class RecentTask {
        @JsonProperty("id")
        private String id;

        @JsonProperty("title")
        private String title;

        @JsonProperty("timestamp")
        private String timestamp;  // ISO timestamp string
    }
}