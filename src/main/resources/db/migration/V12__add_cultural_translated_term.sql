ALTER TABLE article_cultural_term
    ADD COLUMN translated_term VARCHAR(100) NULL AFTER source_term;

DELETE FROM article_cultural_analysis;
