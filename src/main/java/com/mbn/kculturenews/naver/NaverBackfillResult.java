package com.mbn.kculturenews.naver;

import java.time.Instant;

public record NaverBackfillResult(
        String keyword,
        int requestedPages,
        int fetchedCount,
        int mbnCount,
        int savedCount,
        int skippedCount,
        Instant collectedAt
) {
}
