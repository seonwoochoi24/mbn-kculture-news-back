package com.mbn.kculturenews.translation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArticleLocalizationRepository extends JpaRepository<ArticleLocalization, Long> {

    void deleteByArticleArticleId(long articleId);

    Optional<ArticleLocalization> findByArticleArticleIdAndLanguageCodeAndTranslationStatus(
            long articleId,
            String languageCode,
            TranslationStatus translationStatus
    );
}
