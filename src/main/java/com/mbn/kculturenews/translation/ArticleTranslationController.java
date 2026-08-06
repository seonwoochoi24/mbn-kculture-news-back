package com.mbn.kculturenews.translation;

import com.mbn.kculturenews.article.ArticleQueryService;
import com.mbn.kculturenews.article.ArticleResponse;
import com.mbn.kculturenews.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/articles")
@Tag(name = "AI 번역", description = "OpenAI를 이용한 기사 번역 생성 및 캐시 API")
@SecurityRequirement(name = "AdminKey")
public class ArticleTranslationController {

    private final ArticleQueryService articleQueryService;

    public ArticleTranslationController(ArticleQueryService articleQueryService) {
        this.articleQueryService = articleQueryService;
    }

    @PostMapping("/{articleId}/translations")
    @Operation(
            summary = "기사 번역 생성",
            description = "번역 캐시가 없으면 gpt-4o-mini로 생성해 저장하고, 이미 있으면 저장된 번역을 반환합니다."
    )
    public ApiResponse<ArticleResponse> translate(
            @PathVariable @Min(1) long articleId,
            @Parameter(description = "번역 대상 언어(en, ja, zh)", example = "en")
            @RequestParam String language
    ) {
        SupportedLanguage targetLanguage = SupportedLanguage.fromCode(language);
        if (targetLanguage == SupportedLanguage.KOREAN) {
            throw new UnsupportedLanguageException(language);
        }
        return ApiResponse.success(articleQueryService.findById(articleId, targetLanguage.getCode()));
    }
}
