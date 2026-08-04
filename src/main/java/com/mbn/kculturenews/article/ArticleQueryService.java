package com.mbn.kculturenews.article;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ArticleQueryService {

    private final ArticleRepository articleRepository;

    public ArticleQueryService(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    public PageResponse<ArticleResponse> findAll(String keyword, int page, int size) {
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "publishedAt", "articleId")
        );

        Page<Article> articles;
        if (keyword == null || keyword.isBlank()) {
            articles = articleRepository.findByStatus(ArticleStatus.PUBLISHED, pageable);
        } else {
            articles = articleRepository.searchByKeyword(
                    ArticleStatus.PUBLISHED,
                    keyword.trim(),
                    pageable
            );
        }

        return PageResponse.from(articles.map(ArticleResponse::from));
    }

    public ArticleResponse findById(long articleId) {
        Article article = articleRepository.findById(articleId)
                .filter(found -> found.getStatus() == ArticleStatus.PUBLISHED)
                .orElseThrow(() -> new ArticleNotFoundException(articleId));
        return ArticleResponse.from(article);
    }
}
