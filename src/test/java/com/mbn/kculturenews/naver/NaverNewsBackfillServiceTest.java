package com.mbn.kculturenews.naver;

import com.mbn.kculturenews.ingestion.ArticleIngestionService;
import com.mbn.kculturenews.ingestion.ExternalArticleCandidate;
import com.mbn.kculturenews.ingestion.IngestionOutcome;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NaverNewsBackfillServiceTest {

    @Test
    void savesOnlyMbnArticlesAndCountsDuplicates() {
        NaverNewsClient client = mock(NaverNewsClient.class);
        ArticleIngestionService ingestionService = mock(ArticleIngestionService.class);
        NaverNewsBackfillService service = new NaverNewsBackfillService(client, ingestionService);

        when(client.search("김채원", 1, 100)).thenReturn(new NaverNewsPage(
                3,
                1,
                3,
                List.of(
                        new NaverNewsItem(
                                "<b>김채원</b> 첫 기사",
                                "https://www.mbn.co.kr/news/1",
                                "https://n.news.naver.com/1",
                                "첫 번째 기사",
                                "Tue, 04 Aug 2026 10:00:00 +0900"
                        ),
                        new NaverNewsItem(
                                "타 언론사 기사",
                                "https://example.com/news/2",
                                "https://n.news.naver.com/2",
                                "저장하면 안 되는 기사",
                                "Tue, 04 Aug 2026 09:00:00 +0900"
                        ),
                        new NaverNewsItem(
                                "김채원 중복 기사",
                                "https://m.mbn.co.kr/news/3",
                                "https://n.news.naver.com/3",
                                "이미 저장된 기사",
                                "Tue, 04 Aug 2026 08:00:00 +0900"
                        )
                )
        ));
        when(ingestionService.ingest(any(ExternalArticleCandidate.class)))
                .thenReturn(IngestionOutcome.SAVED, IngestionOutcome.DUPLICATE);

        NaverBackfillResult result = service.backfill("김채원", 3);

        assertThat(result.requestedPages()).isEqualTo(1);
        assertThat(result.fetchedCount()).isEqualTo(3);
        assertThat(result.mbnCount()).isEqualTo(2);
        assertThat(result.savedCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isEqualTo(1);
        verify(client).search("김채원", 1, 100);
    }
}
