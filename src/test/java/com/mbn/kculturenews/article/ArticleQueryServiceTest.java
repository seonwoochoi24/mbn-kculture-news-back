package com.mbn.kculturenews.article;

import com.mbn.kculturenews.translation.ArticleLocalization;
import com.mbn.kculturenews.translation.ArticleTranslationService;
import com.mbn.kculturenews.translation.SupportedLanguage;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArticleQueryServiceTest {

    @Test
    void naturalLanguageQueryUsesAiKeywordsAndSortsByRelevance() {
        ArticleRepository repository = mock(ArticleRepository.class);
        NewsAiService newsAiService = mock(NewsAiService.class);
        Article strong = article(1L, "BTS 팬덤 반응", "그래미 보이콧에 아미가 동참했다.", "핵심 요약");
        Article weak = article(2L, "그래미 시상식", "후보가 발표됐다.", null);

        when(newsAiService.extractKeywords("그래미와 BTS 팬덤 반응 관련 기사"))
                .thenReturn(Optional.of(List.of("BTS", "그래미", "아미")));
        when(repository.findByStatus(ArticleStatus.PUBLISHED)).thenReturn(List.of(weak, strong));

        ArticleQueryService service = new ArticleQueryService(
                repository,
                mock(ArticleTranslationService.class),
                newsAiService
        );

        PageResponse<ArticleResponse> result = service.findAll(
                null,
                "그래미와 BTS 팬덤 반응 관련 기사",
                "ko",
                0,
                20
        );

        assertThat(result.content()).extracting(ArticleResponse::articleId).containsExactly(1L, 2L);
        assertThat(result.content().getFirst().summary()).isEqualTo("핵심 요약");
        assertThat(result.content().getFirst().content()).isEqualTo("그래미 보이콧에 아미가 동참했다.");
        verify(newsAiService).populateSummaries(List.of(strong, weak));
    }

    @Test
    void fallsBackToExistingKeywordSearchWhenAiExtractionFails() {
        ArticleRepository repository = mock(ArticleRepository.class);
        NewsAiService newsAiService = mock(NewsAiService.class);
        Article article = article(3L, "BTS 기사", "그래미 관련 본문", null);
        when(newsAiService.extractKeywords("원본 자연어 검색"))
                .thenReturn(Optional.empty());
        when(repository.searchByKeyword(
                eq(ArticleStatus.PUBLISHED),
                eq("원본 자연어 검색"),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(article)));

        ArticleQueryService service = new ArticleQueryService(
                repository,
                mock(ArticleTranslationService.class),
                newsAiService
        );

        PageResponse<ArticleResponse> result = service.findAll(null, "원본 자연어 검색", "ko", 0, 20);

        assertThat(result.content()).extracting(ArticleResponse::articleId).containsExactly(3L);
        verify(repository).searchByKeyword(
                eq(ArticleStatus.PUBLISHED),
                eq("원본 자연어 검색"),
                any(Pageable.class)
        );
    }

    @Test
    void translatesListItemsWhenLanguageIsNotKorean() {
        ArticleRepository repository = mock(ArticleRepository.class);
        NewsAiService newsAiService = mock(NewsAiService.class);
        ArticleTranslationService translationService = mock(ArticleTranslationService.class);
        Article article = article(4L, "BTS 기사", "한국어 본문", "한국어 요약");
        ArticleLocalization localization = mock(ArticleLocalization.class);

        when(repository.findByStatus(eq(ArticleStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(article)));
        when(translationService.translate(article, SupportedLanguage.ENGLISH)).thenReturn(localization);
        when(localization.getTranslatedTitle()).thenReturn("BTS article");
        when(localization.getTranslatedContent()).thenReturn("English content");
        when(localization.getTranslatedSummary()).thenReturn("English summary");
        when(localization.getLanguageCode()).thenReturn("en");

        ArticleQueryService service = new ArticleQueryService(repository, translationService, newsAiService);

        PageResponse<ArticleResponse> result = service.findAll(null, null, "en", 0, 20);

        ArticleResponse response = result.content().getFirst();
        assertThat(response.title()).isEqualTo("BTS article");
        assertThat(response.content()).isEqualTo("English content");
        assertThat(response.summary()).isEqualTo("English summary");
        assertThat(response.languageCode()).isEqualTo("en");
        verify(translationService).translate(article, SupportedLanguage.ENGLISH);
    }

    private Article article(long id, String title, String content, String summary) {
        Article article = mock(Article.class);
        when(article.getArticleId()).thenReturn(id);
        when(article.getTitle()).thenReturn(title);
        when(article.getContent()).thenReturn(content);
        when(article.getSummary()).thenReturn(summary);
        when(article.getContentType()).thenReturn(ContentType.NEWS);
        when(article.getSourceName()).thenReturn("MBN");
        when(article.getSourceCategory()).thenReturn("ENTERTAINMENT");
        when(article.getSourceUrl()).thenReturn("https://www.mbn.co.kr/news/entertain/" + id);
        when(article.getOriginalLanguageCode()).thenReturn("ko");
        when(article.getPublishedAt()).thenReturn(Instant.parse("2026-08-01T00:00:00Z").plusSeconds(id));
        when(article.getCollectedAt()).thenReturn(Instant.parse("2026-08-02T00:00:00Z"));
        return article;
    }
}
