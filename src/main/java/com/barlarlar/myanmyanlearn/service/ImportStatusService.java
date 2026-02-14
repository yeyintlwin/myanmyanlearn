package com.barlarlar.myanmyanlearn.service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class ImportStatusService {

    public enum ImportState {
        PENDING,
        UPLOADING,
        PROCESSING,
        COMPLETED,
        FAILED
    }

    public record ImportJobStatus(
            String jobId,
            ImportState state,
            int progressPercent,
            String message,
            Map<String, Object> result,
            Instant lastUpdated) {
    }

    private final Map<String, ImportJobStatus> jobs = new ConcurrentHashMap<>();

    public void createJob(String jobId) {
        jobs.put(jobId,
                new ImportJobStatus(jobId, ImportState.PENDING, 0, "Initializing...", null, Instant.now()));
    }

    public void updateStatus(String jobId, ImportState state, int progress, String message) {
        updateStatus(jobId, state, progress, message, null);
    }

    public void updateStatus(String jobId, ImportState state, int progress, String message,
            Map<String, Object> result) {
        if (jobId == null || !jobs.containsKey(jobId)) {
            return;
        }
        jobs.put(jobId, new ImportJobStatus(jobId, state, progress, message, result, Instant.now()));
    }

    public ImportJobStatus getJob(String jobId) {
        return jobs.get(jobId);
    }

    public void cleanupOldJobs() {
        // Simple cleanup logic if needed, e.g., remove jobs older than 1 hour
        Instant cutoff = Instant.now().minusSeconds(3600);
        jobs.entrySet().removeIf(entry -> entry.getValue().lastUpdated().isBefore(cutoff));
    }
}
