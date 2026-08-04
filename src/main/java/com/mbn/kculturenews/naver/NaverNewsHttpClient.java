package com.mbn.kculturenews.naver;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class NaverNewsHttpClient implements NaverNewsClient {

    private final NaverNewsProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public NaverNewsHttpClient(NaverNewsProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    @Override
    public NaverNewsPage search(String keyword, int start, int display) {
        if (!properties.hasCredentials()) {
            throw new NaverConfigurationException();
        }

        try {
            String responseBody = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search/v1/news")
                            .queryParam("query", keyword)
                            .queryParam("display", display)
                            .queryParam("start", start)
                            .queryParam("sort", "sim")
                            .queryParam("format", "json")
                            .build())
                    .header("X-NCP-APIGW-API-KEY-ID", properties.getClientId())
                    .header("X-NCP-APIGW-API-KEY", properties.getClientSecret())
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                throw new NaverNewsApiException("네이버 뉴스 API가 빈 응답을 반환했습니다.", null);
            }
            return objectMapper.readValue(responseBody, NaverNewsPage.class);
        } catch (NaverNewsApiException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            String responseBody = sanitizeResponseBody(exception.getResponseBodyAsString());
            String message = "네이버 뉴스 API가 HTTP " + exception.getStatusCode().value() + "을 반환했습니다.";
            if (!responseBody.isBlank()) {
                message += " 응답: " + responseBody;
            }
            throw new NaverNewsApiException(message, exception);
        } catch (JacksonException exception) {
            throw new NaverNewsApiException(
                    "네이버 뉴스 API JSON 응답을 해석하지 못했습니다. 원인: " + sanitizeDetail(exception.getMessage()),
                    exception
            );
        } catch (RestClientException exception) {
            Throwable rootCause = exception.getMostSpecificCause();
            String detail = rootCause == null ? exception.getClass().getSimpleName() : rootCause.getMessage();
            throw new NaverNewsApiException(
                    "네이버 뉴스 API 연결 또는 응답 처리에 실패했습니다. 원인: " + sanitizeDetail(detail),
                    exception
            );
        }
    }

    private String sanitizeResponseBody(String value) {
        if (value == null) {
            return "";
        }
        String compact = value.replaceAll("[\\r\\n\\t]+", " ").trim();
        return compact.length() <= 500 ? compact : compact.substring(0, 500);
    }

    private String sanitizeDetail(String value) {
        if (value == null || value.isBlank()) {
            return "알 수 없는 오류";
        }
        String compact = value.replaceAll("[\\r\\n\\t]+", " ").trim();
        return compact.length() <= 300 ? compact : compact.substring(0, 300);
    }
}
