package com.mbn.kculturenews.ingestion;

import java.time.Instant;

public record InitialBackfillResponse(
        long jobId,
        String status,
        int targetCount,
        int batchSize,
        long currentOriginalArticleCount,
        int savedCount,
        int scannedCount,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt
) {
    public static InitialBackfillResponse from(InitialBackfillJob job, long currentCount) {
        return new InitialBackfillResponse(
                job.getInitialBackfillJobId(),
                job.getStatus().name(),
                job.getTargetCount(),
                job.getBatchSize(),
                currentCount,
                job.getSavedCount(),
                job.getScannedCount(),
                job.getErrorMessage(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }
}
