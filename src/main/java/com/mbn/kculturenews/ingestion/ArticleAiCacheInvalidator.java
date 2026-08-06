package com.mbn.kculturenews.ingestion;

import com.mbn.kculturenews.culture.ArticleCulturalAnalysisRepository;
import com.mbn.kculturenews.translation.ArticleLocalizationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ArticleAiCacheInvalidator {

    private final ArticleLocalizationRepository localizationRepository;
    private final ArticleCulturalAnalysisRepository culturalAnalysisRepository;

    public ArticleAiCacheInvalidator(
            ArticleLocalizationRepository localizationRepository,
            ArticleCulturalAnalysisRepository culturalAnalysisRepository
    ) {
        this.localizationRepository = localizationRepository;
        this.culturalAnalysisRepository = culturalAnalysisRepository;
    }

    @Transactional
    public void invalidate(long articleId) {
        culturalAnalysisRepository.deleteByArticleArticleId(articleId);
        localizationRepository.deleteByArticleArticleId(articleId);
    }
}
