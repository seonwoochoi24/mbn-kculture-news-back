package com.mbn.kculturenews.ingestion;

import com.mbn.kculturenews.article.Article;
import com.mbn.kculturenews.article.ArticleRepository;
import com.mbn.kculturenews.rss.MbnArticleUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ArticleIngestionService {

    private static final Logger log = LoggerFactory.getLogger(ArticleIngestionService.class);

    private final ArticleRepository articleRepository;

    public ArticleIngestionService(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    public IngestionOutcome ingest(ExternalArticleCandidate candidate) {
        if (candidate.sourceUrl() == null || candidate.sourceUrl().isBlank()
                || candidate.title() == null || candidate.title().isBlank()) {
            return IngestionOutcome.REJECTED;
        }

        final String normalizedUrl;
        try {
            normalizedUrl = MbnArticleUrl.normalize(candidate.sourceUrl());
        } catch (IllegalArgumentException exception) {
            log.debug("MBN 도메인이 아닌 기사를 건너뜁니다: {}", candidate.sourceUrl());
            return IngestionOutcome.REJECTED;
        }

        String urlHash = MbnArticleUrl.sha256(normalizedUrl);
        if (articleRepository.existsBySourceUrlHash(urlHash)) {
            return IngestionOutcome.DUPLICATE;
        }
        if (candidate.externalGuid() != null
                && articleRepository.existsBySourceNameAndExternalGuid("MBN", candidate.externalGuid())) {
            return IngestionOutcome.DUPLICATE;
        }

        Instant collectedAt = Instant.now();
        Article article = Article.fromExternalSource(
                normalizedUrl,
                urlHash,
                truncate(candidate.externalGuid(), 500),
                truncate(candidate.title(), 500),
                candidate.description(),
                candidate.publishedAt(),
                collectedAt
        );

        try {
            articleRepository.saveAndFlush(article);
            return IngestionOutcome.SAVED;
        } catch (DataIntegrityViolationException exception) {
            log.debug("동시에 수집된 중복 기사를 건너뜁니다: {}", normalizedUrl);
            return IngestionOutcome.DUPLICATE;
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
