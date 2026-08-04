package com.mbn.kculturenews.naver;

import java.util.List;

public record NaverNewsPage(
        int total,
        int start,
        int display,
        List<NaverNewsItem> items
) {
}
