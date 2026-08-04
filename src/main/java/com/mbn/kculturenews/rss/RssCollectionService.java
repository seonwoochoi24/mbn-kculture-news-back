package com.mbn.kculturenews.rss;

import com.mbn.kculturenews.ingestion.ArticleIngestionService;
import com.mbn.kculturenews.ingestion.ExternalArticleCandidate;
import com.mbn.kculturenews.ingestion.IngestionOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class RssCollectionService {

    private static final Logger log = LoggerFactory.getLogger(RssCollectionService.class);

    private final RssFeedClient rssFeedClient;
    private final RssProperties rssProperties;
    private final ArticleIngestionService articleIngestionService;

    public RssCollectionService(
            RssFeedClient rssFeedClient,
            RssProperties rssProperties,
            ArticleIngestionService articleIngestionService
    ) {
        this.rssFeedClient = rssFeedClient;
        this.rssProperties = rssProperties;
        this.articleIngestionService = articleIngestionService;
    }

    public RssCollectionResult collectEntertainment() {
        List<RssItem> items = rssFeedClient.fetch(rssProperties.getEntertainmentUrl());
        Instant collectedAt = Instant.now();
        int savedCount = 0;

        for (RssItem item : items) {
            IngestionOutcome outcome = articleIngestionService.ingest(new ExternalArticleCandidate(
                    item.guid(),
                    item.link(),
                    item.title(),
                    item.description(),
                    item.publishedAt()
            ));
            if (outcome == IngestionOutcome.SAVED) {
                savedCount++;
            }
        }

        int skippedCount = items.size() - savedCount;
        log.info("MBN 연예 RSS 수집 완료: fetched={}, saved={}, skipped={}",
                items.size(), savedCount, skippedCount);
        return new RssCollectionResult(items.size(), savedCount, skippedCount, collectedAt);
    }

}
