package com.mbn.kculturenews.timeline;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "article_timeline_cache")
public class ArticleTimelineCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long timelineCacheId;

    @Column(nullable = false, unique = true, length = 64)
    private String cacheKey;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String responseJson;

    @Column(nullable = false)
    private Instant generatedAt;

    protected ArticleTimelineCache() {
    }

    public ArticleTimelineCache(String cacheKey, String responseJson, Instant generatedAt) {
        this.cacheKey = cacheKey;
        this.responseJson = responseJson;
        this.generatedAt = generatedAt;
    }

    public String getResponseJson() {
        return responseJson;
    }
}
