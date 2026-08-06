package com.mbn.kculturenews.timeline;

import java.time.LocalDate;

public record TimelineArticleInput(long articleId, String title, String content, LocalDate publishedDate) {
}
