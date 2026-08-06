package com.mbn.kculturenews.timeline;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record TimelineRequest(@NotEmpty List<Long> articleIds) {
}
