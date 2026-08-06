CREATE TABLE initial_backfill_job (
    initial_backfill_job_id BIGINT NOT NULL AUTO_INCREMENT,
    status VARCHAR(20) NOT NULL,
    target_count INT NOT NULL,
    batch_size INT NOT NULL,
    saved_count INT NOT NULL DEFAULT 0,
    scanned_count INT NOT NULL DEFAULT 0,
    query_index INT NOT NULL DEFAULT 0,
    naver_start INT NOT NULL DEFAULT 1,
    error_message VARCHAR(1000) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (initial_backfill_job_id),
    CONSTRAINT chk_initial_backfill_status
        CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED')),
    INDEX idx_initial_backfill_status_created (status, created_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
