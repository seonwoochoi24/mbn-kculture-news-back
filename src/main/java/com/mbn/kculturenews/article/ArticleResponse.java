package com.mbn.kculturenews.article;

import java.time.Instant;

public record ArticleResponse(
        long articleId,
        String title,
        String description,
        String contentType,
        String sourceName,
        String sourceCategory,
        String sourceUrl,
        String languageCode,
        Instant publishedAt,
        Instant collectedAt
) {
    public static ArticleResponse from(Article article) {
        return new ArticleResponse(
                article.getArticleId(),
                article.getTitle(),
                article.getDescription(),
                article.getContentType().name(),
                article.getSourceName(),
                article.getSourceCategory(),
                article.getSourceUrl(),
                article.getOriginalLanguageCode(),
                article.getPublishedAt(),
                article.getCollectedAt()
        );
    }
}
