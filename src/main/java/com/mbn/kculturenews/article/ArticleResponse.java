package com.mbn.kculturenews.article;

import com.mbn.kculturenews.translation.ArticleLocalization;

import java.time.Instant;

public record ArticleResponse(
        long articleId,
        String title,
        String content,
        String summary,
        String imageUrl,
        String journalistName,
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
                article.getContent(),
                article.getSummary(),
                article.getImageUrl(),
                article.getJournalistName(),
                article.getContentType().name(),
                article.getSourceName(),
                article.getSourceCategory(),
                article.getSourceUrl(),
                article.getOriginalLanguageCode(),
                article.getPublishedAt(),
                article.getCollectedAt()
        );
    }

    public static ArticleResponse from(Article article, ArticleLocalization localization) {
        return new ArticleResponse(
                article.getArticleId(),
                localization.getTranslatedTitle(),
                localization.getTranslatedContent(),
                localization.getTranslatedSummary(),
                article.getImageUrl(),
                article.getJournalistName(),
                article.getContentType().name(),
                article.getSourceName(),
                article.getSourceCategory(),
                article.getSourceUrl(),
                localization.getLanguageCode(),
                article.getPublishedAt(),
                article.getCollectedAt()
        );
    }

}
