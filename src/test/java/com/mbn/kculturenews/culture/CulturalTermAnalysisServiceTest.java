package com.mbn.kculturenews.culture;

import com.mbn.kculturenews.article.Article;
import com.mbn.kculturenews.article.ArticleRepository;
import com.mbn.kculturenews.article.ArticleStatus;
import com.mbn.kculturenews.translation.OpenAiProperties;
import com.mbn.kculturenews.translation.SupportedLanguage;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CulturalTermAnalysisServiceTest {

    @Test
    void returnsCachedAnalysisWithoutCallingOpenAi() {
        ArticleRepository articleRepository = mock(ArticleRepository.class);
        ArticleCulturalAnalysisRepository analysisRepository = mock(ArticleCulturalAnalysisRepository.class);
        CulturalTermClient client = mock(CulturalTermClient.class);
        Article article = article("막내가 애교를 선보였다.");
        ArticleCulturalAnalysis cached = ArticleCulturalAnalysis.completed(
                article,
                SupportedLanguage.ENGLISH,
                List.of(new CulturalTermResult(
                        "막내", "youngest member", "maknae", "The youngest member of a group."
                )),
                "gpt-4o-mini",
                "cultural-term-v1",
                Instant.parse("2026-08-05T00:00:00Z")
        );

        when(articleRepository.findById(1L)).thenReturn(Optional.of(article));
        when(analysisRepository.findByArticleArticleIdAndLanguageCodeAndAnalysisStatus(
                1L, "en", CulturalAnalysisStatus.COMPLETED
        )).thenReturn(Optional.of(cached));

        CulturalTermResponse response = service(articleRepository, analysisRepository, client)
                .analyze(1L, "en");

        assertThat(response.terms()).hasSize(1);
        assertThat(response.terms().getFirst().term()).isEqualTo("youngest member");
        assertThat(response.terms().getFirst().sourceTerm()).isEqualTo("막내");
        assertThat(response.terms().getFirst().romanization()).isEqualTo("maknae");
        verify(client, never()).extract(any(), any(), any());
    }

    @Test
    void filtersHallucinatedAndDuplicateTermsBeforeSaving() {
        ArticleRepository articleRepository = mock(ArticleRepository.class);
        ArticleCulturalAnalysisRepository analysisRepository = mock(ArticleCulturalAnalysisRepository.class);
        CulturalTermClient client = mock(CulturalTermClient.class);
        Article article = article("막내가 애교를 선보였다.");

        when(articleRepository.findById(1L)).thenReturn(Optional.of(article));
        when(analysisRepository.findByArticleArticleIdAndLanguageCodeAndAnalysisStatus(
                1L, "en", CulturalAnalysisStatus.COMPLETED
        )).thenReturn(Optional.empty());
        when(client.extract(any(), any(), any())).thenReturn(List.of(
                new CulturalTermResult("막내", "youngest member", "maknae", "The youngest member of a group."),
                new CulturalTermResult("막내", "youngest member", "maknae", "Duplicate."),
                new CulturalTermResult("입덕", "becoming a fan", "ipdeok", "Not present in this article."),
                new CulturalTermResult("애교", "cuteness", "aegyo", "Deliberately cute behavior.")
        ));
        when(analysisRepository.saveAndFlush(any(ArticleCulturalAnalysis.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CulturalTermResponse response = service(articleRepository, analysisRepository, client)
                .analyze(1L, "en");

        assertThat(response.terms())
                .extracting(CulturalTermResponse.Term::term)
                .containsExactly("youngest member", "cuteness");
        assertThat(response.terms())
                .extracting(CulturalTermResponse.Term::sourceTerm)
                .containsExactly("막내", "애교");
        verify(analysisRepository).saveAndFlush(any(ArticleCulturalAnalysis.class));
    }

    @Test
    void cachesAnEmptyAnalysisWhenNoCulturalTermsExist() {
        ArticleRepository articleRepository = mock(ArticleRepository.class);
        ArticleCulturalAnalysisRepository analysisRepository = mock(ArticleCulturalAnalysisRepository.class);
        CulturalTermClient client = mock(CulturalTermClient.class);
        Article article = article("신곡이 오늘 공개됐다.");

        when(articleRepository.findById(1L)).thenReturn(Optional.of(article));
        when(analysisRepository.findByArticleArticleIdAndLanguageCodeAndAnalysisStatus(
                1L, "ja", CulturalAnalysisStatus.COMPLETED
        )).thenReturn(Optional.empty());
        when(client.extract(any(), any(), any())).thenReturn(List.of());
        when(analysisRepository.saveAndFlush(any(ArticleCulturalAnalysis.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CulturalTermResponse response = service(articleRepository, analysisRepository, client)
                .analyze(1L, "ja");

        assertThat(response.terms()).isEmpty();
        verify(analysisRepository).saveAndFlush(any(ArticleCulturalAnalysis.class));
    }

    private CulturalTermAnalysisService service(
            ArticleRepository articleRepository,
            ArticleCulturalAnalysisRepository analysisRepository,
            CulturalTermClient client
    ) {
        return new CulturalTermAnalysisService(
                articleRepository,
                analysisRepository,
                client,
                new OpenAiProperties()
        );
    }

    private Article article(String description) {
        Article article = mock(Article.class);
        when(article.getArticleId()).thenReturn(1L);
        when(article.getStatus()).thenReturn(ArticleStatus.PUBLISHED);
        when(article.getTitle()).thenReturn("아이돌 인터뷰");
        when(article.getContent()).thenReturn(description);
        return article;
    }
}
