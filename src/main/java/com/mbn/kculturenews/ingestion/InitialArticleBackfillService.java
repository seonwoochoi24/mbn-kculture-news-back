package com.mbn.kculturenews.ingestion;

import com.mbn.kculturenews.article.ArticleRepository;
import com.mbn.kculturenews.naver.NaverNewsClient;
import com.mbn.kculturenews.naver.NaverNewsItem;
import com.mbn.kculturenews.naver.NaverNewsPage;
import com.mbn.kculturenews.rss.MbnArticleUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class InitialArticleBackfillService {

    private static final Logger log = LoggerFactory.getLogger(InitialArticleBackfillService.class);
    private static final int MAX_NAVER_START = 1000;
    private static final List<String> DISCOVERY_QUERIES = List.of(
            "연예", "K팝", "아이돌", "가수", "배우", "드라마", "예능", "콘서트", "BTS", "한류"
    );

    private final InitialBackfillJobRepository jobRepository;
    private final ArticleRepository articleRepository;
    private final NaverNewsClient naverNewsClient;
    private final ArticleIngestionService ingestionService;

    public InitialArticleBackfillService(
            InitialBackfillJobRepository jobRepository,
            ArticleRepository articleRepository,
            NaverNewsClient naverNewsClient,
            ArticleIngestionService ingestionService
    ) {
        this.jobRepository = jobRepository;
        this.articleRepository = articleRepository;
        this.naverNewsClient = naverNewsClient;
        this.ingestionService = ingestionService;
    }

    public InitialBackfillResponse start(int targetCount, int batchSize) {
        InitialBackfillJob job = jobRepository
                .findFirstByStatusOrderByCreatedAtAsc(InitialBackfillStatus.RUNNING)
                .orElseGet(() -> jobRepository.saveAndFlush(
                        InitialBackfillJob.start(targetCount, batchSize, Instant.now())
                ));
        completeIfTargetReached(job);
        return response(job);
    }

    public InitialBackfillResponse find(long jobId) {
        InitialBackfillJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new InitialBackfillJobNotFoundException(jobId));
        return response(job);
    }

    @Scheduled(fixedDelayString = "${app.initial-backfill.interval-ms:5000}")
    public void processNextBatch() {
        jobRepository.findFirstByStatusOrderByCreatedAtAsc(InitialBackfillStatus.RUNNING)
                .ifPresent(this::process);
    }

    private void process(InitialBackfillJob job) {
        if (completeIfTargetReached(job)) {
            return;
        }
        if (job.getQueryIndex() >= DISCOVERY_QUERIES.size()) {
            job.fail("검색 후보를 모두 확인했지만 원문 기사 " + job.getTargetCount() + "개를 확보하지 못했습니다.", Instant.now());
            jobRepository.saveAndFlush(job);
            return;
        }

        String query = DISCOVERY_QUERIES.get(job.getQueryIndex());
        try {
            NaverNewsPage page = naverNewsClient.search(query, job.getNaverStart(), job.getBatchSize());
            List<NaverNewsItem> items = page.items() == null ? List.of() : page.items();
            int saved = 0;
            for (NaverNewsItem item : items) {
                if (articleRepository.countByContentFetchedAtIsNotNull() >= job.getTargetCount()) {
                    break;
                }
                if (!MbnArticleUrl.isMbnUrl(item.originalLink())) {
                    continue;
                }
                IngestionOutcome outcome = ingestionService.ingest(new ExternalArticleCandidate(
                        null,
                        item.originalLink(),
                        cleanText(item.title()),
                        null,
                        parsePublishedAt(item.pubDate())
                ));
                if (outcome == IngestionOutcome.SAVED) {
                    saved++;
                }
            }

            int nextStart = job.getNaverStart() + items.size();
            job.recordBatch(items.size(), saved, nextStart, Instant.now());
            boolean queryFinished = items.size() < job.getBatchSize()
                    || nextStart > MAX_NAVER_START
                    || nextStart > page.total();
            if (queryFinished) {
                job.moveToNextQuery(Instant.now());
            }
            completeIfTargetReached(job);
            jobRepository.saveAndFlush(job);
        } catch (RuntimeException exception) {
            log.error("초기 원문 수집 배치 실패: jobId={}, query={}",
                    job.getInitialBackfillJobId(), query, exception);
            job.fail(exception.getMessage(), Instant.now());
            jobRepository.saveAndFlush(job);
        }
    }

    private boolean completeIfTargetReached(InitialBackfillJob job) {
        if (articleRepository.countByContentFetchedAtIsNotNull() < job.getTargetCount()) {
            return false;
        }
        if (job.getStatus() == InitialBackfillStatus.RUNNING) {
            job.complete(Instant.now());
            jobRepository.saveAndFlush(job);
        }
        return true;
    }

    private InitialBackfillResponse response(InitialBackfillJob job) {
        return InitialBackfillResponse.from(job, articleRepository.countByContentFetchedAtIsNotNull());
    }

    private String cleanText(String value) {
        if (value == null) {
            return null;
        }
        return HtmlUtils.htmlUnescape(value.replaceAll("<[^>]+>", " "))
                .replaceAll("\\s+", " ")
                .trim();
    }

    private Instant parsePublishedAt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
        } catch (DateTimeParseException exception) {
            return null;
        }
    }
}
