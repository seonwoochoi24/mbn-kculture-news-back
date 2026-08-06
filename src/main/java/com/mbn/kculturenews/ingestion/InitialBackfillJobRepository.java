package com.mbn.kculturenews.ingestion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InitialBackfillJobRepository extends JpaRepository<InitialBackfillJob, Long> {

    Optional<InitialBackfillJob> findFirstByStatusOrderByCreatedAtAsc(InitialBackfillStatus status);
}
