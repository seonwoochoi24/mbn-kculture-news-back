package com.mbn.kculturenews.naver;

import com.mbn.kculturenews.ingestion.ArticleIngestionService;
import com.mbn.kculturenews.ingestion.ExternalArticleCandidate;
import com.mbn.kculturenews.ingestion.IngestionOutcome;
import com.mbn.kculturenews.rss.MbnArticleUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class NaverNewsBackfillService {

    private static final Logger log = LoggerFactory.getLogger(NaverNewsBackfillService.class);
    private static final int DISPLAY = 100;
    private static final int MAX_START = 1000;

    private final NaverNewsClient naverNewsClient;
    private final ArticleIngestionService articleIngestionService;

    public NaverNewsBackfillService(
            NaverNewsClient naverNewsClient,
            ArticleIngestionService articleIngestionService
    ) {
        this.naverNewsClient = naverNewsClient;
        this.articleIngestionService = articleIngestionService;
    }

    public NaverBackfillResult backfill(String keyword, int maxPages) {
        int fetchedCount = 0;
        int mbnCount = 0;
        int savedCount = 0;
        int requestedPages = 0;

        for (int pageIndex = 0; pageIndex < maxPages; pageIndex++) {
            int start = pageIndex * DISPLAY + 1;
            if (start > MAX_START) {
                break;
            }

            NaverNewsPage page = naverNewsClient.search(keyword, start, DISPLAY);
            requestedPages++;
            List<NaverNewsItem> items = page.items() == null ? List.of() : page.items();
            fetchedCount += items.size();

            for (NaverNewsItem item : items) {
                if (!MbnArticleUrl.isMbnUrl(item.originalLink())) {
                    continue;
                }
                mbnCount++;
                IngestionOutcome outcome = articleIngestionService.ingest(new ExternalArticleCandidate(
                        null,
                        item.originalLink(),
                        cleanText(item.title()),
                        cleanText(item.description()),
                        parsePublishedAt(item.pubDate())
                ));
                if (outcome == IngestionOutcome.SAVED) {
                    savedCount++;
                }
            }

            if (items.size() < DISPLAY || start + items.size() > page.total()) {
                break;
            }
        }

        int skippedCount = mbnCount - savedCount;
        log.info("네이버 MBN 기사 백필 완료: keyword={}, pages={}, fetched={}, mbn={}, saved={}, skipped={}",
                keyword, requestedPages, fetchedCount, mbnCount, savedCount, skippedCount);
        return new NaverBackfillResult(
                keyword,
                requestedPages,
                fetchedCount,
                mbnCount,
                savedCount,
                skippedCount,
                Instant.now()
        );
    }

    private String cleanText(String value) {
        if (value == null) {
            return null;
        }
        String withoutTags = value.replaceAll("<[^>]+>", " ");
        return HtmlUtils.htmlUnescape(withoutTags).replaceAll("\\s+", " ").trim();
    }

    private Instant parsePublishedAt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
        } catch (DateTimeParseException exception) {
            log.warn("네이버 기사 발행시각을 해석하지 못했습니다: {}", value);
            return null;
        }
    }
}
