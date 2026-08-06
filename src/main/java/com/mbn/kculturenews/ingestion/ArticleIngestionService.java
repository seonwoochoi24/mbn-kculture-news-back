package com.mbn.kculturenews.ingestion;

import com.mbn.kculturenews.article.Article;
import com.mbn.kculturenews.article.ArticleRepository;
import com.mbn.kculturenews.rss.MbnArticleUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.net.URI;
import java.util.Optional;

@Service
public class ArticleIngestionService {

    private static final Logger log = LoggerFactory.getLogger(ArticleIngestionService.class);

    private final ArticleRepository articleRepository;
    private final ArticleMetadataClient articleMetadataClient;
    private final ArticleAiCacheInvalidator cacheInvalidator;

    public ArticleIngestionService(
            ArticleRepository articleRepository,
            ArticleMetadataClient articleMetadataClient,
            ArticleAiCacheInvalidator cacheInvalidator
    ) {
        this.articleRepository = articleRepository;
        this.articleMetadataClient = articleMetadataClient;
        this.cacheInvalidator = cacheInvalidator;
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
        Optional<Article> duplicateByUrl = articleRepository.findBySourceUrlHash(urlHash);
        if (duplicateByUrl.isPresent()) {
            enrichDuplicate(duplicateByUrl.get(), normalizedUrl);
            return IngestionOutcome.DUPLICATE;
        }
        if (candidate.externalGuid() != null) {
            Optional<Article> duplicateByGuid = articleRepository.findBySourceNameAndExternalGuid(
                    "MBN",
                    candidate.externalGuid()
            );
            if (duplicateByGuid.isPresent()) {
                enrichDuplicate(duplicateByGuid.get(), normalizedUrl);
                return IngestionOutcome.DUPLICATE;
            }
        }

        MbnArticleMetadata metadata = fetchMetadata(normalizedUrl);
        if (metadata.content() == null || metadata.content().isBlank()) {
            log.warn("MBN 원문 본문을 추출하지 못해 저장하지 않습니다: {}", normalizedUrl);
            return IngestionOutcome.REJECTED;
        }
        Instant collectedAt = Instant.now();
        Article article = Article.fromExternalSource(
                normalizedUrl,
                urlHash,
                truncate(candidate.externalGuid(), 500),
                truncate(candidate.title(), 500),
                metadata.content(),
                truncate(metadata.imageUrl(), 2048),
                truncate(metadata.journalistName(), 200),
                metadata.publishedAt() == null ? candidate.publishedAt() : metadata.publishedAt(),
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

    private MbnArticleMetadata fetchMetadata(String sourceUrl) {
        try {
            return articleMetadataClient.fetch(URI.create(sourceUrl));
        } catch (RuntimeException exception) {
            log.warn("기사 메타데이터 추출에 실패해 본문 정보만 저장합니다: {}", sourceUrl, exception);
            return MbnArticleMetadata.empty();
        }
    }

    private void enrichDuplicate(Article article, String sourceUrl) {
        if (article.getContentFetchedAt() != null) {
            return;
        }
        MbnArticleMetadata metadata = fetchMetadata(sourceUrl);
        boolean contentChanged = metadata.content() != null
                && !metadata.content().equals(article.getContent());
        if (article.applySourceDocument(
                metadata.content(),
                truncate(metadata.imageUrl(), 2048),
                truncate(metadata.journalistName(), 200),
                metadata.publishedAt(),
                Instant.now()
        )) {
            articleRepository.saveAndFlush(article);
            if (contentChanged) {
                cacheInvalidator.invalidate(article.getArticleId());
            }
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
