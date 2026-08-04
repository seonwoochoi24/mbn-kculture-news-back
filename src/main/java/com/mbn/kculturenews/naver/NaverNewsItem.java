package com.mbn.kculturenews.naver;

public record NaverNewsItem(
        String title,
        String originallink,
        String link,
        String description,
        String pubDate
) {
    public String originalLink() {
        return originallink;
    }
}
