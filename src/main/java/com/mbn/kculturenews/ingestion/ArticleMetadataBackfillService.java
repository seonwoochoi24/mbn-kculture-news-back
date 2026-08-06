package com.mbn.kculturenews.ingestion;

import com.mbn.kculturenews.article.Article;
import com.mbn.kculturenews.article.ArticleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@Service
public class ArticleMetadataBackfillService {

    private static final Logger log = LoggerFactory.getLogger(ArticleMetadataBackfillService.class);

    private final ArticleRepository articleRepository;
    private final ArticleMetadataClient metadataClient;
    private final ArticleAiCacheInvalidator cacheInvalidator;

    public ArticleMetadataBackfillService(
            ArticleRepository articleRepository,
            ArticleMetadataClient metadataClient,
            ArticleAiCacheInvalidator cacheInvalidator
    ) {
        this.articleRepository = articleRepository;
        this.metadataClient = metadataClient;
        this.cacheInvalidator = cacheInvalidator;
    }

    public MetadataBackfillResult backfill(int batchSize) {
        List<Article> articles = articleRepository.findByContentFetchedAtIsNullOrderByArticleIdAsc(
                PageRequest.of(0, batchSize)
        );
        int updatedCount = 0;
        int failedCount = 0;

        for (Article article : articles) {
            try {
                MbnArticleMetadata metadata = metadataClient.fetch(URI.create(article.getSourceUrl()));
                boolean contentChanged = metadata.content() != null
                        && !metadata.content().equals(article.getContent());
                if (article.applySourceDocument(
                        metadata.content(),
                        metadata.imageUrl(),
                        metadata.journalistName(),
                        metadata.publishedAt(),
                        Instant.now()
                )) {
                    articleRepository.saveAndFlush(article);
                    if (contentChanged) {
                        cacheInvalidator.invalidate(article.getArticleId());
                    }
                    updatedCount++;
                } else {
                    failedCount++;
                }
            } catch (RuntimeException exception) {
                failedCount++;
                log.warn("기사 메타데이터 백필 실패: articleId={}, url={}",
                        article.getArticleId(), article.getSourceUrl(), exception);
            }
        }

        return new MetadataBackfillResult(
                articles.size(),
                updatedCount,
                failedCount,
                articleRepository.countByContentFetchedAtIsNull()
        );
    }
}
