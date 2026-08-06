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

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(length = 500)
    private String summary;

    private Instant summaryGeneratedAt;

    private Instant contentFetchedAt;

    @Column(length = 2048)
    private String imageUrl;

    @Column(length = 200)
    private String journalistName;

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
            String content,
            String imageUrl,
            String journalistName,
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
        article.content = content;
        article.contentFetchedAt = collectedAt;
        article.imageUrl = imageUrl;
        article.journalistName = journalistName;
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

    public String getContent() {
        return content;
    }

    public Instant getContentFetchedAt() {
        return contentFetchedAt;
    }

    public String getSummary() {
        return summary;
    }

    public void cacheSummary(String summary, Instant generatedAt) {
        this.summary = summary;
        this.summaryGeneratedAt = generatedAt;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getJournalistName() {
        return journalistName;
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

    public boolean applySourceDocument(
            String content,
            String imageUrl,
            String journalistName,
            Instant originalPublishedAt,
            Instant changedAt
    ) {
        boolean changed = false;
        if (content != null && !content.equals(this.content)) {
            this.content = content;
            this.contentFetchedAt = changedAt;
            this.summary = null;
            this.summaryGeneratedAt = null;
            changed = true;
        }
        if (this.imageUrl == null && imageUrl != null) {
            this.imageUrl = imageUrl;
            changed = true;
        }
        if (this.journalistName == null && journalistName != null) {
            this.journalistName = journalistName;
            changed = true;
        }
        if (originalPublishedAt != null && !originalPublishedAt.equals(this.publishedAt)) {
            this.publishedAt = originalPublishedAt;
            changed = true;
        }
        if (changed) {
            this.updatedAt = changedAt;
        }
        return changed;
    }
}
