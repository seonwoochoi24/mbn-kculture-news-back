package com.mbn.kculturenews.article;

public class ArticleNotFoundException extends RuntimeException {

    public ArticleNotFoundException(long articleId) {
        super("기사를 찾을 수 없습니다: " + articleId);
    }
}
