ALTER TABLE article
    ADD COLUMN summary VARCHAR(500) NULL AFTER content,
    ADD COLUMN summary_generated_at DATETIME(6) NULL AFTER summary;
