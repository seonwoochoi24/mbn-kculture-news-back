package com.mbn.kculturenews.ingestion;

public record MetadataBackfillResult(
        int processedCount,
        int updatedCount,
        int failedCount,
        long remainingWithoutContent
) {
}
