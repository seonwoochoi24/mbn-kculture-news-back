package com.mbn.kculturenews.ingestion;

import com.mbn.kculturenews.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/articles/content")
@Tag(name = "기사 원문 보충", description = "기존 기사의 원문·대표 이미지·기자명·발행일 보충 API")
@SecurityRequirement(name = "AdminKey")
public class ArticleMetadataBackfillController {

    private final ArticleMetadataBackfillService backfillService;

    public ArticleMetadataBackfillController(ArticleMetadataBackfillService backfillService) {
        this.backfillService = backfillService;
    }

    @PostMapping("/backfill")
    @Operation(
            summary = "기존 기사 원문 보충",
            description = "원문을 아직 가져오지 않은 기존 기사를 ID 순서로 처리해 본문, 이미지, 기자명, 발행일을 채웁니다."
    )
    public ApiResponse<MetadataBackfillResult> backfill(
            @Parameter(description = "한 번에 처리할 기사 수(1~10)", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(10) int batchSize
    ) {
        return ApiResponse.success(backfillService.backfill(batchSize));
    }
}
