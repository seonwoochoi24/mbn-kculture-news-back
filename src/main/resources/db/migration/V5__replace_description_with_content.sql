ALTER TABLE article
    CHANGE COLUMN description content LONGTEXT NULL,
    ADD COLUMN content_fetched_at DATETIME(6) NULL AFTER content;
