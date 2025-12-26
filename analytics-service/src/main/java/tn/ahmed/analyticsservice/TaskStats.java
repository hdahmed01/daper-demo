package tn.ahmed.analyticsservice;



import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Map;

/**
 * TaskStats Model - Represents aggregated statistics
 *
 * This object is stored in Dapr State Store (Redis) with key "global-task-stats"
 * and updated every time a task-created event is received.
 *
 * Example JSON:
 * {
 *   "totalTasks": 150,
 *   "tasksToday": 12,
 *   "lastUpdated": "2024-12-26T15:30:45.123",
 *   "tasksByTitle": {
 *     "Buy groceries": 35,
 *     "Write report": 22,
 *     "Call client": 18
 *   }
 * }
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TaskStats {

    /**
     * Total number of tasks created since the beginning
     */
    @JsonProperty("totalTasks")
    private int totalTasks;

    /**
     * Number of tasks created today (reset daily at midnight)
     */
    @JsonProperty("tasksToday")
    private int tasksToday;

    /**
     * Last time the stats were updated (ISO 8601 format)
     */
    @JsonProperty("lastUpdated")
    private String lastUpdated;

    /**
     * Distribution of tasks by title
     * Key: Task title
     * Value: Number of tasks with that title
     */
    @JsonProperty("tasksByTitle")
    private Map<String, Integer> tasksByTitle;
}