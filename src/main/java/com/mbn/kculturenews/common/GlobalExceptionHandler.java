package com.mbn.kculturenews.common;

import com.mbn.kculturenews.article.ArticleNotFoundException;
import com.mbn.kculturenews.naver.NaverConfigurationException;
import com.mbn.kculturenews.naver.NaverNewsApiException;
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
}
