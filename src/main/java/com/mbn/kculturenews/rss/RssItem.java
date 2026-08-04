package com.mbn.kculturenews.rss;

import java.time.Instant;

public record RssItem(
        String guid,
        String link,
        String title,
        String description,
        Instant publishedAt
) {
}
