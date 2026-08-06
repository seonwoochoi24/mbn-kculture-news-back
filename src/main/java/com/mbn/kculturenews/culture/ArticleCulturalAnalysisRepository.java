package com.mbn.kculturenews.culture;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArticleCulturalAnalysisRepository
        extends JpaRepository<ArticleCulturalAnalysis, Long> {

    void deleteByArticleArticleId(long articleId);

    @EntityGraph(attributePaths = "terms")
    Optional<ArticleCulturalAnalysis> findByArticleArticleIdAndLanguageCodeAndAnalysisStatus(
            long articleId,
            String languageCode,
            CulturalAnalysisStatus analysisStatus
    );
}
