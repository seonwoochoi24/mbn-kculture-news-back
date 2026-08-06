package com.mbn.kculturenews.timeline;

import java.util.List;

public record TimelineResponse(List<TimelineItemResponse> items) {

    public static TimelineResponse empty() {
        return new TimelineResponse(List.of());
    }
}
