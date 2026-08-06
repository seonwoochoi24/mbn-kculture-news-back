CREATE TABLE article_localization (
    article_localization_id BIGINT NOT NULL AUTO_INCREMENT,
    article_id BIGINT NOT NULL,
    language_code VARCHAR(10) NOT NULL,
    translated_title VARCHAR(500) NOT NULL,
    translated_description TEXT NULL,
    translation_status VARCHAR(20) NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    prompt_version VARCHAR(50) NOT NULL,
    generated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (article_localization_id),
    CONSTRAINT uk_article_localization_language UNIQUE (article_id, language_code),
    CONSTRAINT fk_article_localization_article
        FOREIGN KEY (article_id) REFERENCES article (article_id) ON DELETE CASCADE,
    CONSTRAINT chk_article_localization_language CHECK (language_code IN ('en', 'ja', 'zh')),
    CONSTRAINT chk_article_localization_status
        CHECK (translation_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    INDEX idx_article_localization_language (language_code, article_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
