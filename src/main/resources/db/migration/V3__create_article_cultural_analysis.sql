CREATE TABLE article_cultural_analysis (
    cultural_analysis_id BIGINT NOT NULL AUTO_INCREMENT,
    article_id BIGINT NOT NULL,
    language_code VARCHAR(10) NOT NULL,
    analysis_status VARCHAR(20) NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    prompt_version VARCHAR(50) NOT NULL,
    generated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (cultural_analysis_id),
    CONSTRAINT uk_cultural_analysis_article_language UNIQUE (article_id, language_code),
    CONSTRAINT fk_cultural_analysis_article
        FOREIGN KEY (article_id) REFERENCES article (article_id) ON DELETE CASCADE,
    CONSTRAINT chk_cultural_analysis_language CHECK (language_code IN ('en', 'ja', 'zh')),
    CONSTRAINT chk_cultural_analysis_status CHECK (analysis_status IN ('COMPLETED')),
    INDEX idx_cultural_analysis_language (language_code, article_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE article_cultural_term (
    cultural_term_id BIGINT NOT NULL AUTO_INCREMENT,
    cultural_analysis_id BIGINT NOT NULL,
    source_term VARCHAR(50) NOT NULL,
    romanization VARCHAR(100) NOT NULL,
    explanation VARCHAR(1000) NOT NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (cultural_term_id),
    CONSTRAINT uk_cultural_term_source UNIQUE (cultural_analysis_id, source_term),
    CONSTRAINT fk_cultural_term_analysis
        FOREIGN KEY (cultural_analysis_id)
        REFERENCES article_cultural_analysis (cultural_analysis_id) ON DELETE CASCADE,
    INDEX idx_cultural_term_analysis_order (cultural_analysis_id, sort_order)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
