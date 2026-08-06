package com.mbn.kculturenews.ingestion;

public class InitialBackfillJobNotFoundException extends RuntimeException {

    public InitialBackfillJobNotFoundException(long jobId) {
        super("초기 원문 수집 작업을 찾을 수 없습니다: " + jobId);
    }
}
