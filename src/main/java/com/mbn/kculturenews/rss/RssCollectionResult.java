package com.mbn.kculturenews.rss;

import java.time.Instant;

public record RssCollectionResult(
        int fetchedCount,
        int savedCount,
        int skippedCount,
        Instant collectedAt
) {
}
