package com.mbn.kculturenews.article;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    boolean existsBySourceUrlHash(String sourceUrlHash);

    boolean existsBySourceNameAndExternalGuid(String sourceName, String externalGuid);

    Page<Article> findByStatus(ArticleStatus status, Pageable pageable);

    @Query("""
            SELECT article
              FROM Article article
             WHERE article.status = :status
               AND (
                    LOWER(article.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(COALESCE(article.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               )
            """)
    Page<Article> searchByKeyword(
            @Param("status") ArticleStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
