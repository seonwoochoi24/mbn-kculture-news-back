package com.mbn.kculturenews.article;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(
        name = "article",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_article_source_url_hash", columnNames = "source_url_hash"),
                @UniqueConstraint(name = "uk_article_source_guid", columnNames = {"source_name", "external_guid"})
        }
)
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long articleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContentType contentType;

    @Column(nullable = false, length = 50)
    private String sourceName;

    @Column(nullable = false, length = 2048)
    private String sourceUrl;

    @Column(nullable = false, length = 64)
    private String sourceUrlHash;

    @Column(length = 500)
    private String externalGuid;

    @Column(nullable = false, length = 50)
    private String sourceCategory;

    @Column(nullable = false, length = 10)
    private String originalLanguageCode;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Instant publishedAt;

    @Column(nullable = false)
    private Instant collectedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ArticleStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected Article() {
    }

    public static Article fromExternalSource(
            String sourceUrl,
            String sourceUrlHash,
            String externalGuid,
            String title,
            String description,
            Instant publishedAt,
            Instant collectedAt
    ) {
        Article article = new Article();
        article.contentType = ContentType.NEWS;
        article.sourceName = "MBN";
        article.sourceUrl = sourceUrl;
        article.sourceUrlHash = sourceUrlHash;
        article.externalGuid = externalGuid;
        article.sourceCategory = "ENTERTAINMENT";
        article.originalLanguageCode = "ko";
        article.title = title;
        article.description = description;
        article.publishedAt = publishedAt;
        article.collectedAt = collectedAt;
        article.status = ArticleStatus.PUBLISHED;
        article.createdAt = collectedAt;
        article.updatedAt = collectedAt;
        return article;
    }

    public Long getArticleId() {
        return articleId;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getExternalGuid() {
        return externalGuid;
    }

    public String getSourceCategory() {
        return sourceCategory;
    }

    public String getOriginalLanguageCode() {
        return originalLanguageCode;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public Instant getCollectedAt() {
        return collectedAt;
    }

    public ArticleStatus getStatus() {
        return status;
    }
}
