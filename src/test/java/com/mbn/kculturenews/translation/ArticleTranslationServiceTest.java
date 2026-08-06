package com.mbn.kculturenews.translation;

import com.mbn.kculturenews.article.Article;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArticleTranslationServiceTest {

    @Test
    void returnsCachedTranslationWithoutCallingOpenAi() {
        ArticleLocalizationRepository repository = mock(ArticleLocalizationRepository.class);
        TranslationClient client = mock(TranslationClient.class);
        OpenAiProperties properties = new OpenAiProperties();
        Article article = article();
        ArticleLocalization cached = localization(article, "Cached title", "Cached description");

        when(repository.findByArticleArticleIdAndLanguageCodeAndTranslationStatus(
                article.getArticleId(),
                "en",
                TranslationStatus.COMPLETED
        )).thenReturn(Optional.of(cached));

        ArticleTranslationService service = new ArticleTranslationService(repository, client, properties);

        ArticleLocalization result = service.translate(article, SupportedLanguage.ENGLISH);

        assertThat(result).isSameAs(cached);
        verify(client, never()).translate(any(), any(), any(), any());
    }

    @Test
    void generatesAndSavesTranslationOnCacheMiss() {
        ArticleLocalizationRepository repository = mock(ArticleLocalizationRepository.class);
        TranslationClient client = mock(TranslationClient.class);
        OpenAiProperties properties = new OpenAiProperties();
        Article article = article();

        when(repository.findByArticleArticleIdAndLanguageCodeAndTranslationStatus(
                article.getArticleId(),
                "ja",
                TranslationStatus.COMPLETED
        )).thenReturn(Optional.empty());
        when(client.translate(article.getTitle(), article.getContent(), article.getSummary(), SupportedLanguage.JAPANESE))
                .thenReturn(new TranslationResult("翻訳タイトル", "翻訳説明", "翻訳要約"));
        when(repository.saveAndFlush(any(ArticleLocalization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ArticleTranslationService service = new ArticleTranslationService(repository, client, properties);

        ArticleLocalization result = service.translate(article, SupportedLanguage.JAPANESE);

        assertThat(result.getLanguageCode()).isEqualTo("ja");
        assertThat(result.getTranslatedTitle()).isEqualTo("翻訳タイトル");
        assertThat(result.getTranslatedContent()).isEqualTo("翻訳説明");
        assertThat(result.getTranslatedSummary()).isEqualTo("翻訳要約");
        assertThat(result.getTranslationStatus()).isEqualTo(TranslationStatus.COMPLETED);
        assertThat(result.getModelName()).isEqualTo("gpt-4o-mini");
        verify(repository).saveAndFlush(any(ArticleLocalization.class));
    }

    private Article article() {
        Article article = mock(Article.class);
        when(article.getArticleId()).thenReturn(1L);
        when(article.getTitle()).thenReturn("한국어 제목");
        when(article.getContent()).thenReturn("한국어 원문");
        when(article.getSummary()).thenReturn("한국어 요약");
        return article;
    }

    private ArticleLocalization localization(Article article, String title, String description) {
        return ArticleLocalization.completed(
                article,
                SupportedLanguage.ENGLISH,
                new TranslationResult(title, description, "Cached summary"),
                "gpt-4o-mini",
                "translation-v1",
                Instant.parse("2026-08-05T00:02:00Z")
        );
    }
}
