package tn.ahmed.taskservice.service;



import io.dapr.client.DaprClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.ahmed.taskservice.entities.Task;

@Service
public class TaskCacheService {

    @Autowired
    private DaprClient daprClient;

    private static final String CACHE_STORE = "statestore"; // Redis

    // Get from cache
    public Task getCachedTask(String id) {
        try {
            String key = "task:" + id;
            var state = daprClient.getState(CACHE_STORE, key, Task.class).block();

            if (state != null && state.getValue() != null) {
                System.out.println("✅ Cache HIT: " + key);
                return state.getValue();
            }

            System.out.println("❌ Cache MISS: " + key);
            return null;
        } catch (Exception e) {
            System.err.println("Cache read error: " + e.getMessage());
            return null;
        }
    }

    // Save to cache
    public void cacheTask(Task task) {
        try {
            String key = "task:" + task.getId();
            daprClient.saveState(CACHE_STORE, key, task).block();
            System.out.println("💾 Cached: " + key);
        } catch (Exception e) {
            System.err.println("Cache write error: " + e.getMessage());
        }
    }

    // Delete from cache
    public void invalidateTask(String id) {
        try {
            String key = "task:" + id;
            daprClient.deleteState(CACHE_STORE, key).block();
            System.out.println("🗑️  Cache invalidated: " + key);
        } catch (Exception e) {
            System.err.println("Cache delete error: " + e.getMessage());
        }
    }
}