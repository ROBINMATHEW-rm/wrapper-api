package com.enterprise_wrapper_api.wrapper_api.rag;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks the status of an async PDF upload job.
 */
public class UploadJob {

    public enum Status {
        QUEUED, PROCESSING, COMPLETED, FAILED
    }

    private final String jobId;
    private final String documentId;
    private final String filename;
    private final LocalDateTime createdAt;

    private volatile Status status;
    private volatile String errorMessage;
    private volatile int totalChunks;
    private final AtomicInteger processedChunks = new AtomicInteger(0);
    private volatile LocalDateTime completedAt;

    public UploadJob(String jobId, String documentId, String filename) {
        this.jobId = jobId;
        this.documentId = documentId;
        this.filename = filename;
        this.createdAt = LocalDateTime.now();
        this.status = Status.QUEUED;
    }

    public String getJobId() { return jobId; }
    public String getDocumentId() { return documentId; }
    public String getFilename() { return filename; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Status getStatus() { return status; }
    public String getErrorMessage() { return errorMessage; }
    public int getTotalChunks() { return totalChunks; }
    public int getProcessedChunks() { return processedChunks.get(); }
    public LocalDateTime getCompletedAt() { return completedAt; }

    public void setStatus(Status status) { this.status = status; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public void setTotalChunks(int totalChunks) { this.totalChunks = totalChunks; }
    public void incrementProcessedChunks() { this.processedChunks.incrementAndGet(); }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public int getProgressPercent() {
        if (totalChunks == 0) return 0;
        return (int) ((processedChunks.get() * 100.0) / totalChunks);
    }
}
