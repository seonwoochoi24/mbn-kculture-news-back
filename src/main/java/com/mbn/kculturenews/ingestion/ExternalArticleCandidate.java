package com.mbn.kculturenews.ingestion;

import java.time.Instant;

public record ExternalArticleCandidate(
        String externalGuid,
        String sourceUrl,
        String title,
        String description,
        Instant publishedAt
) {
}
