package com.mbn.kculturenews.common;

import com.mbn.kculturenews.article.ArticleNotFoundException;
import com.mbn.kculturenews.culture.OpenAiCulturalTermException;
import com.mbn.kculturenews.culture.UnsupportedCulturalTermLanguageException;
import com.mbn.kculturenews.ingestion.InitialBackfillJobNotFoundException;
import com.mbn.kculturenews.naver.NaverConfigurationException;
import com.mbn.kculturenews.naver.NaverNewsApiException;
import com.mbn.kculturenews.timeline.InvalidTimelineRequestException;
import com.mbn.kculturenews.translation.OpenAiConfigurationException;
import com.mbn.kculturenews.translation.OpenAiTranslationException;
import com.mbn.kculturenews.translation.UnsupportedLanguageException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ArticleNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ArticleNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("ARTICLE_NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler(InitialBackfillJobNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleBackfillJobNotFound(InitialBackfillJobNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("INITIAL_BACKFILL_JOB_NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler({ConstraintViolationException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<ApiResponse<Void>> handleValidation(Exception exception) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("INVALID_INPUT_VALUE", "요청값이 올바르지 않습니다."));
    }

    @ExceptionHandler(RssCollectionException.class)
    public ResponseEntity<ApiResponse<Void>> handleRssCollection(RssCollectionException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.error("RSS_COLLECTION_FAILED", exception.getMessage()));
    }

    @ExceptionHandler(NaverConfigurationException.class)
    public ResponseEntity<ApiResponse<Void>> handleNaverConfiguration(NaverConfigurationException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error("NAVER_API_NOT_CONFIGURED", exception.getMessage()));
    }

    @ExceptionHandler(NaverNewsApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleNaverApi(NaverNewsApiException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.error("NAVER_API_FAILED", exception.getMessage()));
    }

    @ExceptionHandler(UnsupportedLanguageException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnsupportedLanguage(UnsupportedLanguageException exception) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("UNSUPPORTED_LANGUAGE", exception.getMessage()));
    }

    @ExceptionHandler(OpenAiConfigurationException.class)
    public ResponseEntity<ApiResponse<Void>> handleOpenAiConfiguration(OpenAiConfigurationException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error("OPENAI_API_NOT_CONFIGURED", exception.getMessage()));
    }

    @ExceptionHandler(OpenAiTranslationException.class)
    public ResponseEntity<ApiResponse<Void>> handleOpenAiTranslation(OpenAiTranslationException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.error("OPENAI_TRANSLATION_FAILED", exception.getMessage()));
    }

    @ExceptionHandler(UnsupportedCulturalTermLanguageException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnsupportedCulturalLanguage(
            UnsupportedCulturalTermLanguageException exception
    ) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("UNSUPPORTED_CULTURAL_TERM_LANGUAGE", exception.getMessage()));
    }

    @ExceptionHandler(OpenAiCulturalTermException.class)
    public ResponseEntity<ApiResponse<Void>> handleOpenAiCulturalTerm(OpenAiCulturalTermException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.error("OPENAI_CULTURAL_TERM_FAILED", exception.getMessage()));
    }

    @ExceptionHandler(InvalidTimelineRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidTimelineRequest(InvalidTimelineRequestException exception) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("INVALID_TIMELINE_REQUEST", exception.getMessage()));
    }
}
