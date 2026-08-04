package com.mbn.kculturenews.article;

import com.mbn.kculturenews.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/news")
@Tag(name = "뉴스", description = "수집된 MBN 연예 기사 조회 API")
public class ArticleController {

    private final ArticleQueryService articleQueryService;

    public ArticleController(ArticleQueryService articleQueryService) {
        this.articleQueryService = articleQueryService;
    }

    @GetMapping
    @Operation(summary = "기사 목록 및 검색", description = "최신 기사 목록을 조회하거나 제목과 설명에서 키워드를 검색합니다.")
    public ApiResponse<PageResponse<ArticleResponse>> findAll(
            @Parameter(description = "검색어(2~50자)", example = "BTS")
            @RequestParam(required = false) @Size(min = 2, max = 50) String keyword,
            @Parameter(description = "0부터 시작하는 페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "페이지 크기(1~100)", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(articleQueryService.findAll(keyword, page, size));
    }

    @GetMapping("/{articleId}")
    @Operation(summary = "기사 상세 조회", description = "기사 ID로 공개된 기사 한 건을 조회합니다.")
    public ApiResponse<ArticleResponse> findById(@PathVariable @Min(1) long articleId) {
        return ApiResponse.success(articleQueryService.findById(articleId));
    }
}
