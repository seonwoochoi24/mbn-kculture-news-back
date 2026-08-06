package com.mbn.kculturenews.timeline;

import com.mbn.kculturenews.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/news/timeline")
@Tag(name = "AI 타임라인", description = "관련 기사 기반 대표 사건 타임라인 API")
public class TimelineController {

    private final TimelineService timelineService;

    public TimelineController(TimelineService timelineService) {
        this.timelineService = timelineService;
    }

    @PostMapping
    @Operation(summary = "대표 사건 타임라인 생성", description = "articleIds에 포함된 공개 기사만 분석하고 동일 기사 조합은 DB 캐시를 재사용합니다.")
    public ApiResponse<TimelineResponse> create(@Valid @RequestBody TimelineRequest request) {
        return ApiResponse.success(timelineService.create(request.articleIds()));
    }
}
