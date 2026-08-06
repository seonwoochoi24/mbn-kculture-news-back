CREATE TABLE article_timeline_cache (
    timeline_cache_id BIGINT NOT NULL AUTO_INCREMENT,
    cache_key VARCHAR(64) NOT NULL,
    response_json LONGTEXT NOT NULL,
    generated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (timeline_cache_id),
    CONSTRAINT uk_article_timeline_cache_key UNIQUE (cache_key)
);
