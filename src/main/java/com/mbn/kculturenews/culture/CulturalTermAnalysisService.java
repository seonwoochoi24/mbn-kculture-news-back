package com.mbn.kculturenews.culture;

import com.mbn.kculturenews.article.Article;
import com.mbn.kculturenews.article.ArticleNotFoundException;
import com.mbn.kculturenews.article.ArticleRepository;
import com.mbn.kculturenews.article.ArticleStatus;
import com.mbn.kculturenews.translation.OpenAiProperties;
import com.mbn.kculturenews.translation.SupportedLanguage;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CulturalTermAnalysisService {

    private static final int MAX_TERMS = 10;

    private final ArticleRepository articleRepository;
    private final ArticleCulturalAnalysisRepository analysisRepository;
    private final CulturalTermClient culturalTermClient;
    private final OpenAiProperties properties;

    public CulturalTermAnalysisService(
            ArticleRepository articleRepository,
            ArticleCulturalAnalysisRepository analysisRepository,
            CulturalTermClient culturalTermClient,
            OpenAiProperties properties
    ) {
        this.articleRepository = articleRepository;
        this.analysisRepository = analysisRepository;
        this.culturalTermClient = culturalTermClient;
        this.properties = properties;
    }

    public CulturalTermResponse analyze(long articleId, String languageCode) {
        SupportedLanguage language = SupportedLanguage.fromCode(languageCode);
        if (language == SupportedLanguage.KOREAN) {
            throw new UnsupportedCulturalTermLanguageException(languageCode);
        }

        Article article = articleRepository.findById(articleId)
                .filter(found -> found.getStatus() == ArticleStatus.PUBLISHED)
                .orElseThrow(() -> new ArticleNotFoundException(articleId));

        ArticleCulturalAnalysis analysis = findCompleted(articleId, language)
                .orElseGet(() -> generateAndSave(article, language));
        return CulturalTermResponse.from(articleId, analysis);
    }

    private ArticleCulturalAnalysis generateAndSave(Article article, SupportedLanguage language) {
        List<CulturalTermResult> extracted = culturalTermClient.extract(
                article.getTitle(),
                article.getContent(),
                language
        );
        List<CulturalTermResult> validated = validateAgainstArticle(article, extracted);
        ArticleCulturalAnalysis analysis = ArticleCulturalAnalysis.completed(
                article,
                language,
                validated,
                properties.getModel(),
                properties.getCulturalPromptVersion(),
                Instant.now()
        );

        try {
            return analysisRepository.saveAndFlush(analysis);
        } catch (DataIntegrityViolationException exception) {
            return findCompleted(article.getArticleId(), language).orElseThrow(() -> exception);
        }
    }

    private List<CulturalTermResult> validateAgainstArticle(
            Article article,
            List<CulturalTermResult> extracted
    ) {
        String source = article.getTitle() + "\n" + Optional.ofNullable(article.getContent()).orElse("");
        Map<String, CulturalTermResult> unique = new LinkedHashMap<>();
        for (CulturalTermResult raw : extracted) {
            String term = normalized(raw.term());
            String translatedTerm = normalized(raw.translatedTerm());
            String romanization = normalized(raw.romanization());
            String explanation = normalized(raw.explanation());
            if (term.isBlank() || translatedTerm.isBlank() || romanization.isBlank() || explanation.isBlank()) {
                continue;
            }
            if (term.length() > 50 || translatedTerm.length() > 100
                    || romanization.length() > 100 || explanation.length() > 1000) {
                continue;
            }
            if (!source.contains(term)) {
                continue;
            }
            unique.putIfAbsent(term, new CulturalTermResult(term, translatedTerm, romanization, explanation));
            if (unique.size() == MAX_TERMS) {
                break;
            }
        }
        return List.copyOf(unique.values());
    }

    private Optional<ArticleCulturalAnalysis> findCompleted(
            long articleId,
            SupportedLanguage language
    ) {
        return analysisRepository.findByArticleArticleIdAndLanguageCodeAndAnalysisStatus(
                articleId,
                language.getCode(),
                CulturalAnalysisStatus.COMPLETED
        );
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim();
    }
}
