package tn.ahmed.taskservice.repo;



import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import tn.ahmed.taskservice.entities.Priority;
import tn.ahmed.taskservice.entities.Task;
import tn.ahmed.taskservice.entities.TaskStatus;


import java.util.List;

@Repository
public interface TaskRepository extends MongoRepository<Task, String> {

    // Find by status
    List<Task> findByStatus(TaskStatus status);

    // Find by assigned user
    List<Task> findByAssignedTo(String userId);

    // Find by team
    List<Task> findByTeamId(String teamId);

    // Find by priority
    List<Task> findByPriority(Priority priority);

    // Find by status and priority
    List<Task> findByStatusAndPriority(TaskStatus status, Priority priority);

    // Count by status
    long countByStatus(TaskStatus status);

    // Find recent tasks (custom query)
    @Query("{ 'createdAt': { $gte: ?0 } }")
    List<Task> findRecentTasks(String sinceDate);
}