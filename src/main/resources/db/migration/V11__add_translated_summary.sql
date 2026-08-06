ALTER TABLE article_localization
    ADD COLUMN translated_summary VARCHAR(500) NULL AFTER translated_content;
