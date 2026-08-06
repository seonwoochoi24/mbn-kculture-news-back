package com.mbn.kculturenews.culture;

import com.mbn.kculturenews.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/news/{articleId}/cultural-terms")
@Tag(name = "문화 용어", description = "외국인을 위한 한국 문화 용어 추출 및 설명 API")
public class CulturalTermController {

    private final CulturalTermAnalysisService analysisService;

    public CulturalTermController(CulturalTermAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @GetMapping
    @Operation(
            summary = "기사 속 한국 문화 용어 설명",
            description = "기사에 실제 등장하는 문화 용어를 추출해 지정 언어로 설명합니다. 최초 결과는 OpenAI로 생성하고 이후 DB 캐시를 사용합니다."
    )
    public ApiResponse<CulturalTermResponse> findCulturalTerms(
            @PathVariable @Min(1) long articleId,
            @Parameter(description = "설명 언어(en, ja, zh)", example = "en")
            @RequestParam(defaultValue = "en") String language
    ) {
        return ApiResponse.success(analysisService.analyze(articleId, language));
    }
}
