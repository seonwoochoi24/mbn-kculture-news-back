package com.mbn.kculturenews.rss;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.rss", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RssCollectionScheduler {

    private static final Logger log = LoggerFactory.getLogger(RssCollectionScheduler.class);

    private final RssCollectionService rssCollectionService;

    public RssCollectionScheduler(RssCollectionService rssCollectionService) {
        this.rssCollectionService = rssCollectionService;
    }

    @Scheduled(
            fixedDelayString = "${app.rss.interval-ms:300000}",
            initialDelayString = "${app.rss.initial-delay-ms:10000}"
    )
    public void collect() {
        try {
            rssCollectionService.collectEntertainment();
        } catch (RuntimeException exception) {
            log.error("예약된 MBN 연예 RSS 수집에 실패했습니다.", exception);
        }
    }
}
