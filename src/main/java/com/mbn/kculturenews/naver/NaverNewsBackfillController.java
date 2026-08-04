package com.mbn.kculturenews.naver;

import com.mbn.kculturenews.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/naver")
@Tag(name = "네이버 보충 수집", description = "검색어 기반으로 네이버 뉴스에서 MBN 기사만 보충 수집하는 API")
@SecurityRequirement(name = "AdminKey")
public class NaverNewsBackfillController {

    private final NaverNewsBackfillService naverNewsBackfillService;

    public NaverNewsBackfillController(NaverNewsBackfillService naverNewsBackfillService) {
        this.naverNewsBackfillService = naverNewsBackfillService;
    }

    @PostMapping("/backfill")
    @Operation(
            summary = "검색어별 MBN 기사 보충 수집",
            description = "네이버 뉴스 검색 결과 중 원문 URL이 MBN인 기사만 중복 제거 후 저장합니다."
    )
    public ApiResponse<NaverBackfillResult> backfill(
            @Parameter(description = "검색어(2~50자)", example = "김채원")
            @RequestParam @Size(min = 2, max = 50) String keyword,
            @Parameter(description = "조회할 네이버 결과 페이지 수(페이지당 100건)", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(10) int maxPages
    ) {
        return ApiResponse.success(naverNewsBackfillService.backfill(keyword.trim(), maxPages));
    }
}
