package com.mbn.kculturenews.ingestion;

import com.mbn.kculturenews.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/articles/initial-backfill")
@Tag(name = "초기 원문 수집", description = "MBN 원문 기사 100개를 배치로 확보하는 API")
@SecurityRequirement(name = "AdminKey")
public class InitialArticleBackfillController {

    private final InitialArticleBackfillService backfillService;

    public InitialArticleBackfillController(InitialArticleBackfillService backfillService) {
        this.backfillService = backfillService;
    }

    @PostMapping
    @Operation(summary = "초기 원문 수집 시작", description = "네이버에서 MBN 기사 URL을 찾고 5~10개씩 원문을 수집해 총 100개까지 저장합니다.")
    public ApiResponse<InitialBackfillResponse> start(
            @RequestParam(defaultValue = "100") @Min(1) @Max(100) int targetCount,
            @Parameter(description = "한 번에 확인할 검색 결과 수(5~10)", example = "10")
            @RequestParam(defaultValue = "10") @Min(5) @Max(10) int batchSize
    ) {
        return ApiResponse.success(backfillService.start(targetCount, batchSize));
    }

    @GetMapping("/{jobId}")
    @Operation(summary = "초기 원문 수집 진행 상태 조회")
    public ApiResponse<InitialBackfillResponse> find(@PathVariable @Min(1) long jobId) {
        return ApiResponse.success(backfillService.find(jobId));
    }
}
