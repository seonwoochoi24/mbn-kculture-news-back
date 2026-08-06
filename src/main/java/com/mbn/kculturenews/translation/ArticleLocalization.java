package com.mbn.kculturenews.translation;

import com.mbn.kculturenews.article.Article;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
        name = "article_localization",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_article_localization_language",
                columnNames = {"article_id", "language_code"}
        )
)
public class ArticleLocalization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long articleLocalizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Column(nullable = false, length = 10)
    private String languageCode;

    @Column(nullable = false, length = 500)
    private String translatedTitle;

    @Column(columnDefinition = "TEXT")
    private String translatedContent;

    @Column(length = 500)
    private String translatedSummary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TranslationStatus translationStatus;

    @Column(nullable = false, length = 100)
    private String modelName;

    @Column(nullable = false, length = 50)
    private String promptVersion;

    @Column(nullable = false)
    private Instant generatedAt;

    protected ArticleLocalization() {
    }

    public static ArticleLocalization completed(
            Article article,
            SupportedLanguage language,
            TranslationResult result,
            String modelName,
            String promptVersion,
            Instant generatedAt
    ) {
        ArticleLocalization localization = new ArticleLocalization();
        localization.article = article;
        localization.languageCode = language.getCode();
        localization.translatedTitle = result.title();
        localization.translatedContent = result.content();
        localization.translatedSummary = result.summary();
        localization.translationStatus = TranslationStatus.COMPLETED;
        localization.modelName = modelName;
        localization.promptVersion = promptVersion;
        localization.generatedAt = generatedAt;
        return localization;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public String getTranslatedTitle() {
        return translatedTitle;
    }

    public String getTranslatedContent() {
        return translatedContent;
    }

    public String getTranslatedSummary() {
        return translatedSummary;
    }

    public TranslationStatus getTranslationStatus() {
        return translationStatus;
    }

    public String getModelName() {
        return modelName;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }
}
