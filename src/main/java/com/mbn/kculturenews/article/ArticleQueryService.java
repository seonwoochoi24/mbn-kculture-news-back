package com.mbn.kculturenews.article;

import com.mbn.kculturenews.translation.ArticleLocalization;
import com.mbn.kculturenews.translation.ArticleTranslationService;
import com.mbn.kculturenews.translation.SupportedLanguage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class ArticleQueryService {

    private final ArticleRepository articleRepository;
    private final ArticleTranslationService articleTranslationService;
    private final NewsAiService newsAiService;

    public ArticleQueryService(
            ArticleRepository articleRepository,
            ArticleTranslationService articleTranslationService,
            NewsAiService newsAiService
    ) {
        this.articleRepository = articleRepository;
        this.articleTranslationService = articleTranslationService;
        this.newsAiService = newsAiService;
    }

    public PageResponse<ArticleResponse> findAll(String keyword, String query, String languageCode, int page, int size) {
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "publishedAt", "articleId")
        );

        Page<Article> articles;
        if (query != null && !query.isBlank()) {
            String normalizedQuery = query.trim();
            articles = newsAiService.extractKeywords(normalizedQuery)
                    .map(keywords -> searchByKeywords(keywords, page, size))
                    .orElseGet(() -> articleRepository.searchByKeyword(
                            ArticleStatus.PUBLISHED,
                            normalizedQuery,
                            pageable
                    ));
        } else if (keyword == null || keyword.isBlank()) {
            articles = articleRepository.findByStatus(ArticleStatus.PUBLISHED, pageable);
        } else {
            articles = articleRepository.searchByKeyword(
                    ArticleStatus.PUBLISHED,
                    keyword.trim(),
                    pageable
            );
        }

        SupportedLanguage language = SupportedLanguage.fromCode(languageCode);
        if (language == SupportedLanguage.KOREAN) {
            newsAiService.populateSummaries(articles.getContent());
            return PageResponse.from(articles.map(ArticleResponse::from));
        }

        return PageResponse.from(articles.map(article ->
                ArticleResponse.from(article, articleTranslationService.translate(article, language))
        ));
    }

    public ArticleResponse findById(long articleId, String languageCode) {
        Article article = articleRepository.findById(articleId)
                .filter(found -> found.getStatus() == ArticleStatus.PUBLISHED)
                .orElseThrow(() -> new ArticleNotFoundException(articleId));

        SupportedLanguage language = SupportedLanguage.fromCode(languageCode);
        newsAiService.populateSummaries(List.of(article));
        if (language == SupportedLanguage.KOREAN) {
            return ArticleResponse.from(article);
        }

        ArticleLocalization localization = articleTranslationService.translate(article, language);
        return ArticleResponse.from(article, localization);
    }

    private Page<Article> searchByKeywords(List<String> keywords, int page, int size) {
        List<Article> matched = articleRepository.findByStatus(ArticleStatus.PUBLISHED).stream()
                .filter(article -> relevance(article, keywords) >= 2)
                .sorted(Comparator
                        .<Article>comparingInt(article -> relevance(article, keywords))
                        .reversed()
                        .thenComparing(
                                Article::getPublishedAt,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        )
                        .thenComparing(Article::getArticleId, Comparator.reverseOrder()))
                .toList();

        int from = (int) Math.min((long) page * size, matched.size());
        int to = Math.min(from + size, matched.size());
        return new PageImpl<>(matched.subList(from, to), PageRequest.of(page, size), matched.size());
    }

    private int relevance(Article article, List<String> keywords) {
        String title = article.getTitle().toLowerCase(Locale.ROOT);
        String content = article.getContent() == null
                ? ""
                : article.getContent().toLowerCase(Locale.ROOT);
        int score = 0;
        for (String keyword : keywords) {
            String normalized = keyword.toLowerCase(Locale.ROOT);
            if (title.contains(normalized)) {
                score += 3;
            }
            if (content.contains(normalized)) {
                score += 1;
            }
        }
        return score;
    }
}
