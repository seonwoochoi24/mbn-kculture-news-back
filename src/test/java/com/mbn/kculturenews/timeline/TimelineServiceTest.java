package com.mbn.kculturenews.timeline;

import com.mbn.kculturenews.article.Article;
import com.mbn.kculturenews.article.ArticleRepository;
import com.mbn.kculturenews.article.ArticleStatus;
import com.mbn.kculturenews.translation.OpenAiProperties;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TimelineServiceTest {

    @Test
    void validatesArticleIdsAndDatesThenSortsAndCaches() {
        ArticleRepository articleRepository = mock(ArticleRepository.class);
        ArticleTimelineCacheRepository cacheRepository = mock(ArticleTimelineCacheRepository.class);
        TimelineAiClient aiClient = mock(TimelineAiClient.class);
        Article first = article(1L, "7월 25일 티저 공개", "행사는 2026년 7월 25일 열렸다.");
        Article second = article(2L, "8월 1일 컴백", "컴백일은 2026년 8월 1일이다.");
        when(articleRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(first, second));
        when(cacheRepository.findByCacheKey(any())).thenReturn(Optional.empty());
        when(aiClient.extract(any())).thenReturn(List.of(
                new TimelineEvent(LocalDate.of(2026, 8, 1), "컴백", 2L),
                new TimelineEvent(LocalDate.of(2026, 7, 25), "티저 공개", 1L),
                new TimelineEvent(LocalDate.of(2026, 7, 26), "본문에 없는 날짜", 1L),
                new TimelineEvent(LocalDate.of(2026, 7, 25), "없는 기사", 999L)
        ));

        TimelineResponse response = service(articleRepository, cacheRepository, aiClient).create(List.of(1L, 2L));

        assertThat(response.items()).containsExactly(
                new TimelineItemResponse(1L, "티저 공개", "26.07.25"),
                new TimelineItemResponse(2L, "컴백", "26.08.01")
        );
        verify(cacheRepository).saveAndFlush(any(ArticleTimelineCache.class));
    }

    @Test
    void returnsCachedResponseWithoutCallingAi() throws Exception {
        ArticleRepository articleRepository = mock(ArticleRepository.class);
        ArticleTimelineCacheRepository cacheRepository = mock(ArticleTimelineCacheRepository.class);
        TimelineAiClient aiClient = mock(TimelineAiClient.class);
        Article article = article(1L, "2026년 7월 25일 티저", "티저를 공개했다.");
        TimelineResponse cached = new TimelineResponse(
                List.of(new TimelineItemResponse(1L, "티저 공개", "26.07.25"))
        );
        ObjectMapper objectMapper = new ObjectMapper();
        when(articleRepository.findAllById(List.of(1L))).thenReturn(List.of(article));
        when(cacheRepository.findByCacheKey(any())).thenReturn(Optional.of(new ArticleTimelineCache(
                "key", objectMapper.writeValueAsString(cached), Instant.now()
        )));

        TimelineResponse response = service(articleRepository, cacheRepository, aiClient).create(List.of(1L));

        assertThat(response).isEqualTo(cached);
        verify(aiClient, never()).extract(any());
    }

    @Test
    void treatsDifferentArticleIdOrderAsTheSameTimelineInput() {
        ArticleRepository articleRepository = mock(ArticleRepository.class);
        ArticleTimelineCacheRepository cacheRepository = mock(ArticleTimelineCacheRepository.class);
        TimelineAiClient aiClient = mock(TimelineAiClient.class);
        Article first = article(1L, "2026년 7월 25일 티저", "티저를 공개했다.");
        Article second = article(2L, "2026년 8월 1일 컴백", "컴백했다.");
        when(articleRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(first, second));
        when(cacheRepository.findByCacheKey(any())).thenReturn(Optional.of(new ArticleTimelineCache(
                "key",
                "{\"items\":[]}",
                Instant.now()
        )));

        service(articleRepository, cacheRepository, aiClient).create(List.of(2L, 1L));

        verify(articleRepository).findAllById(List.of(1L, 2L));
        verify(aiClient, never()).extract(any());
    }

    @Test
    void returnsEmptyItemsWhenAiFails() {
        ArticleRepository articleRepository = mock(ArticleRepository.class);
        ArticleTimelineCacheRepository cacheRepository = mock(ArticleTimelineCacheRepository.class);
        TimelineAiClient aiClient = mock(TimelineAiClient.class);
        Article article = article(1L, "2026년 7월 25일 티저", "티저를 공개했다.");
        when(articleRepository.findAllById(List.of(1L))).thenReturn(List.of(article));
        when(cacheRepository.findByCacheKey(any())).thenReturn(Optional.empty());
        when(aiClient.extract(any())).thenThrow(new OpenAiTimelineException("fail", new RuntimeException()));

        TimelineResponse response = service(articleRepository, cacheRepository, aiClient).create(List.of(1L));

        assertThat(response.items()).isEmpty();
        verify(cacheRepository, never()).saveAndFlush(any());
    }

    @Test
    void usesPublishedDatesWhenAiEventsDoNotPassArticleDateValidation() {
        ArticleRepository articleRepository = mock(ArticleRepository.class);
        ArticleTimelineCacheRepository cacheRepository = mock(ArticleTimelineCacheRepository.class);
        TimelineAiClient aiClient = mock(TimelineAiClient.class);
        Article first = article(1L, "BTS 그래미 팬덤 반응", "본문에 명시 날짜가 없다.");
        Article second = article(2L, "아미 보이콧 동참", "본문에도 날짜가 없다.");
        when(articleRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(first, second));
        when(cacheRepository.findByCacheKey(any())).thenReturn(Optional.empty());
        when(aiClient.extract(any())).thenReturn(List.of(
                new TimelineEvent(LocalDate.of(2026, 7, 25), "본문에 없는 날짜", 1L)
        ));

        TimelineResponse response = service(articleRepository, cacheRepository, aiClient).create(List.of(1L, 2L));

        assertThat(response.items()).containsExactly(
                new TimelineItemResponse(1L, "BTS 그래미 팬덤 반응", "26.08.01"),
                new TimelineItemResponse(2L, "아미 보이콧 동참", "26.08.01")
        );
    }

    private TimelineService service(
            ArticleRepository articleRepository,
            ArticleTimelineCacheRepository cacheRepository,
            TimelineAiClient aiClient
    ) {
        OpenAiProperties properties = new OpenAiProperties();
        properties.setMaxInputChars(12000);
        return new TimelineService(
                articleRepository, cacheRepository, aiClient, properties, new ObjectMapper()
        );
    }

    private Article article(long id, String title, String content) {
        Article article = mock(Article.class);
        when(article.getArticleId()).thenReturn(id);
        when(article.getTitle()).thenReturn(title);
        when(article.getContent()).thenReturn(content);
        when(article.getPublishedAt()).thenReturn(Instant.parse("2026-08-01T00:00:00Z"));
        when(article.getStatus()).thenReturn(ArticleStatus.PUBLISHED);
        return article;
    }
}
