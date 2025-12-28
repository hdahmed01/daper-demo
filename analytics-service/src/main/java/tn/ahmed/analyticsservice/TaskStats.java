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
    // BASIC COUNTERS
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
    // TIME-BASED TRACKING
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

    @JsonProperty("tasksLastHour")
    @Builder.Default
    private int tasksLastHour = 0;

    @JsonProperty("lastUpdated")
    private String lastUpdated;

    @JsonProperty("lastCountDate")
    private String lastCountDate;

    @JsonProperty("lastTaskDate")
    private String lastTaskDate;

    @JsonProperty("lastHourTimestamp")
    private String lastHourTimestamp;

    // ═════════════════════════════════════════════════════════
    // DISTRIBUTION MAPS
    // ═══════════════════════════════════════════════════════════
    @JsonProperty("tasksByTitle")
    @Builder.Default
    private Map<String, Integer> tasksByTitle = Map.of();

    @JsonProperty("tasksByHour")
    @Builder.Default
    private Map<String, Integer> tasksByHour = Map.of();

    @JsonProperty("tasksByDayOfWeek")
    @Builder.Default
    private Map<String, Integer> tasksByDayOfWeek = Map.of();

    @JsonProperty("tasksByMonth")
    @Builder.Default
    private Map<String, Integer> tasksByMonth = Map.of();

    // ═══════════════════════════════════════════════════════════
    // NEW: TEAM & USER METRICS
    // ═══════════════════════════════════════════════════════════
    @JsonProperty("tasksByPriority")
    @Builder.Default
    private Map<String, Integer> tasksByPriority = Map.of();

    @JsonProperty("tasksByTeam")
    @Builder.Default
    private Map<String, Integer> tasksByTeam = Map.of();

    @JsonProperty("tasksByAssignee")
    @Builder.Default
    private Map<String, Integer> tasksByAssignee = Map.of();

    @JsonProperty("statusTransitions")
    @Builder.Default
    private Map<String, Integer> statusTransitions = Map.of();

    // ═══════════════════════════════════════════════════════════
    // WORD CLOUD & TITLES
    // ═══════════════════════════════════════════════════════════
    @JsonProperty("titleWordFrequency")
    @Builder.Default
    private Map<String, Integer> titleWordFrequency = Map.of();

    @JsonProperty("topTitles")
    @Builder.Default
    private List<String> topTitles = List.of();

    // ═══════════════════════════════════════════════════════════
    // RECENT ACTIVITY
    // ═══════════════════════════════════════════════════════════
    @JsonProperty("recentTasks")
    @Builder.Default
    private List<RecentTask> recentTasks = List.of();

    @JsonProperty("completedTaskIds")
    @Builder.Default
    private List<String> completedTaskIds = List.of();

    // ═══════════════════════════════════════════════════════════
    // STREAKS & PERFORMANCE
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

    @JsonProperty("busiestHour")
    private String busiestHour;

    @JsonProperty("busiestDay")
    private String busiestDay;

    @JsonProperty("peakTasksInOneHour")
    @Builder.Default
    private int peakTasksInOneHour = 0;

    @JsonProperty("averageTasksPerDay")
    @Builder.Default
    private double averageTasksPerDay = 0.0;

    // ═══════════════════════════════════════════════════════════
    // NESTED CLASS: Recent Task
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
        private String timestamp;
    }
}