package com.mbn.kculturenews.timeline;

import java.time.LocalDate;

public record TimelineEvent(LocalDate date, String eventTitle, long articleId) {
}
