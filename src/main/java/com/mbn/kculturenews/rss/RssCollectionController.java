package com.mbn.kculturenews.rss;

import com.mbn.kculturenews.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/rss")
@Tag(name = "RSS 수집", description = "MBN 연예 RSS 수동 수집 API")
@SecurityRequirement(name = "AdminKey")
public class RssCollectionController {

    private final RssCollectionService rssCollectionService;

    public RssCollectionController(RssCollectionService rssCollectionService) {
        this.rssCollectionService = rssCollectionService;
    }

    @PostMapping("/collect")
    @Operation(summary = "MBN 연예 RSS 수동 수집", description = "RSS를 즉시 조회하고 신규 기사만 저장합니다.")
    public ApiResponse<RssCollectionResult> collect() {
        return ApiResponse.success(rssCollectionService.collectEntertainment());
    }
}
