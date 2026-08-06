package com.mbn.kculturenews.timeline;

import java.util.List;

public interface TimelineAiClient {

    List<TimelineEvent> extract(List<TimelineArticleInput> articles);
}
