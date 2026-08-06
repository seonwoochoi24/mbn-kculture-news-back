package com.mbn.kculturenews.ingestion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "initial_backfill_job")
public class InitialBackfillJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long initialBackfillJobId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InitialBackfillStatus status;

    @Column(nullable = false)
    private int targetCount;

    @Column(nullable = false)
    private int batchSize;

    @Column(nullable = false)
    private int savedCount;

    @Column(nullable = false)
    private int scannedCount;

    @Column(nullable = false)
    private int queryIndex;

    @Column(nullable = false)
    private int naverStart;

    @Column(length = 1000)
    private String errorMessage;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected InitialBackfillJob() {
    }

    public static InitialBackfillJob start(int targetCount, int batchSize, Instant now) {
        InitialBackfillJob job = new InitialBackfillJob();
        job.status = InitialBackfillStatus.RUNNING;
        job.targetCount = targetCount;
        job.batchSize = batchSize;
        job.savedCount = 0;
        job.scannedCount = 0;
        job.queryIndex = 0;
        job.naverStart = 1;
        job.createdAt = now;
        job.updatedAt = now;
        return job;
    }

    public void recordBatch(int scanned, int saved, int nextStart, Instant now) {
        this.scannedCount += scanned;
        this.savedCount += saved;
        this.naverStart = nextStart;
        this.updatedAt = now;
    }

    public void moveToNextQuery(Instant now) {
        this.queryIndex++;
        this.naverStart = 1;
        this.updatedAt = now;
    }

    public void complete(Instant now) {
        this.status = InitialBackfillStatus.COMPLETED;
        this.updatedAt = now;
    }

    public void fail(String message, Instant now) {
        this.status = InitialBackfillStatus.FAILED;
        this.errorMessage = message == null ? null : message.substring(0, Math.min(message.length(), 1000));
        this.updatedAt = now;
    }

    public Long getInitialBackfillJobId() {
        return initialBackfillJobId;
    }

    public InitialBackfillStatus getStatus() {
        return status;
    }

    public int getTargetCount() {
        return targetCount;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public int getSavedCount() {
        return savedCount;
    }

    public int getScannedCount() {
        return scannedCount;
    }

    public int getQueryIndex() {
        return queryIndex;
    }

    public int getNaverStart() {
        return naverStart;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
