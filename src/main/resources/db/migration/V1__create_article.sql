CREATE TABLE article (
    article_id BIGINT NOT NULL AUTO_INCREMENT,
    content_type VARCHAR(20) NOT NULL,
    source_name VARCHAR(50) NOT NULL,
    source_url VARCHAR(2048) NOT NULL,
    source_url_hash CHAR(64) NOT NULL,
    external_guid VARCHAR(500) NULL,
    source_category VARCHAR(50) NOT NULL,
    original_language_code VARCHAR(10) NOT NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT NULL,
    published_at DATETIME(6) NULL,
    collected_at DATETIME(6) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (article_id),
    CONSTRAINT uk_article_source_url_hash UNIQUE (source_url_hash),
    CONSTRAINT uk_article_source_guid UNIQUE (source_name, external_guid),
    CONSTRAINT chk_article_content_type CHECK (content_type IN ('NEWS', 'LONG_FORM', 'SHORTS')),
    CONSTRAINT chk_article_status CHECK (status IN ('PUBLISHED', 'DELETED')),
    INDEX idx_article_status_published (status, published_at DESC, article_id DESC)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
