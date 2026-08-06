package com.mbn.kculturenews.ingestion;

import java.time.Instant;

public record MbnArticleMetadata(
        String content,
        String imageUrl,
        String journalistName,
        Instant publishedAt
) {
    public static MbnArticleMetadata empty() {
        return new MbnArticleMetadata(null, null, null, null);
    }
}
