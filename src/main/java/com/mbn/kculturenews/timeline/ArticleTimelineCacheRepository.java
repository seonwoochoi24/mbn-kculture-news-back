package com.mbn.kculturenews.timeline;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArticleTimelineCacheRepository extends JpaRepository<ArticleTimelineCache, Long> {

    Optional<ArticleTimelineCache> findByCacheKey(String cacheKey);
}
