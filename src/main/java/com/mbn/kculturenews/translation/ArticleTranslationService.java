package com.mbn.kculturenews.translation;

import com.mbn.kculturenews.article.Article;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ArticleTranslationService {

    private final ArticleLocalizationRepository localizationRepository;
    private final TranslationClient translationClient;
    private final OpenAiProperties properties;

    public ArticleTranslationService(
            ArticleLocalizationRepository localizationRepository,
            TranslationClient translationClient,
            OpenAiProperties properties
    ) {
        this.localizationRepository = localizationRepository;
        this.translationClient = translationClient;
        this.properties = properties;
    }

    public ArticleLocalization translate(Article article, SupportedLanguage language) {
        if (language == SupportedLanguage.KOREAN) {
            throw new IllegalArgumentException("한국어 원문은 번역 캐시를 생성하지 않습니다.");
        }

        return findCompleted(article.getArticleId(), language)
                .orElseGet(() -> generateAndSave(article, language));
    }

    private ArticleLocalization generateAndSave(Article article, SupportedLanguage language) {
        TranslationResult result = translationClient.translate(
                article.getTitle(),
                article.getContent(),
                article.getSummary(),
                language
        );
        ArticleLocalization localization = ArticleLocalization.completed(
                article,
                language,
                result,
                properties.getModel(),
                properties.getPromptVersion(),
                Instant.now()
        );

        try {
            return localizationRepository.saveAndFlush(localization);
        } catch (DataIntegrityViolationException exception) {
            return findCompleted(article.getArticleId(), language).orElseThrow(() -> exception);
        }
    }

    private java.util.Optional<ArticleLocalization> findCompleted(
            long articleId,
            SupportedLanguage language
    ) {
        return localizationRepository.findByArticleArticleIdAndLanguageCodeAndTranslationStatus(
                articleId,
                language.getCode(),
                TranslationStatus.COMPLETED
        );
    }
}
