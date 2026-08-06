package com.mbn.kculturenews.ingestion;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MbnArticleMetadataClient implements ArticleMetadataClient {

    private static final Pattern META_TAG = Pattern.compile("<meta\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ATTRIBUTE = Pattern.compile(
            "([\\w:-]+)\\s*=\\s*([\\\"'])(.*?)\\2",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern EMAIL_IN_PARENTHESES = Pattern.compile("\\s*\\([^)]*@[^)]*\\)\\s*");
    private static final Pattern EMAIL = Pattern.compile("\\s+[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}.*$");
    private static final Pattern ARTICLE_BODY = Pattern.compile(
            "<div\\b(?=[^>]*\\bid=[\\\"']newsViewArea[\\\"'])[^>]*>(.*?)"
                    + "<!--\\s*//\\s*기사 본문 내용\\s*-->",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern SCRIPT_OR_STYLE = Pattern.compile(
            "<(script|style)\\b[^>]*>.*?</\\1>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern TABLE = Pattern.compile(
            "<table\\b[^>]*>.*?</table>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern REPORTER_FOOTER = Pattern.compile(
            "(?m)^\\s*\\[[^\\n\\]]*기자[^\\n\\]]*@[^\\n\\]]*]\\s*$"
    );

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    public MbnArticleMetadata fetch(URI articleUri) {
        HttpRequest request = HttpRequest.newBuilder(articleUri)
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "text/html")
                .header("User-Agent", "MBN-Knews-MetadataCollector/1.0")
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("MBN 원문이 HTTP " + response.statusCode() + "을 반환했습니다.");
            }
            return parse(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("MBN 원문 메타데이터 요청이 중단되었습니다.", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("MBN 원문 메타데이터를 가져오지 못했습니다.", exception);
        }
    }

    MbnArticleMetadata parse(String html) {
        Map<String, String> metadata = new HashMap<>();
        Matcher tags = META_TAG.matcher(html == null ? "" : html);
        while (tags.find()) {
            Map<String, String> attributes = attributes(tags.group());
            String key = firstNonBlank(
                    attributes.get("property"),
                    attributes.get("name"),
                    attributes.get("itemprop")
            );
            String content = attributes.get("content");
            if (key != null && content != null && !content.isBlank()) {
                metadata.putIfAbsent(key.toLowerCase(Locale.ROOT), decode(content));
            }
        }

        return new MbnArticleMetadata(
                extractContent(html),
                normalizeImageUrl(metadata.get("og:image")),
                normalizeJournalist(metadata.get("article:author")),
                parseInstant(metadata.get("article:published_time"))
        );
    }

    private String extractContent(String html) {
        if (html == null || html.isBlank()) {
            return null;
        }
        Matcher bodyMatcher = ARTICLE_BODY.matcher(html);
        if (!bodyMatcher.find()) {
            return null;
        }

        String body = bodyMatcher.group(1);
        body = SCRIPT_OR_STYLE.matcher(body).replaceAll(" ");
        body = TABLE.matcher(body).replaceAll(" ");
        body = body.replaceAll("(?i)<br\\s*/?>", "\n");
        body = body.replaceAll("(?i)</(p|div|li|h[1-6])\\s*>", "\n");
        body = body.replaceAll("<[^>]+>", " ");
        body = HtmlUtils.htmlUnescape(body);
        body = body.replace('\u00a0', ' ');
        body = REPORTER_FOOTER.matcher(body).replaceAll("");

        StringBuilder normalized = new StringBuilder();
        for (String line : body.split("\\R")) {
            String compact = line.replaceAll("\\s+", " ").trim();
            if (isPageBoilerplate(compact)) {
                continue;
            }
            if (compact.isBlank()) {
                if (!normalized.isEmpty() && normalized.charAt(normalized.length() - 1) != '\n') {
                    normalized.append('\n');
                }
                continue;
            }
            if (!normalized.isEmpty() && normalized.charAt(normalized.length() - 1) != '\n') {
                normalized.append('\n');
            }
            normalized.append(compact).append('\n');
        }
        String result = normalized.toString().trim();
        return result.length() < 50 ? null : result;
    }

    private boolean isPageBoilerplate(String line) {
        return line.equals("VOD 시청 안내")
                || line.startsWith("어도비 플래시 플레이어 서비스 종료에 따라")
                || line.startsWith("현재 브라우저 버전에서는 서비스가 원할하지 않습니다")
                || line.startsWith("아래 버튼을 클릭하셔서 브라우저 업그레이드")
                || line.equals("브라우저 업그레이드 및 설치");
    }

    private Map<String, String> attributes(String tag) {
        Map<String, String> attributes = new HashMap<>();
        Matcher matcher = ATTRIBUTE.matcher(tag);
        while (matcher.find()) {
            attributes.put(matcher.group(1).toLowerCase(Locale.ROOT), matcher.group(3));
        }
        return attributes;
    }

    private String normalizeImageUrl(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.startsWith("http://")) {
            normalized = "https://" + normalized.substring("http://".length());
        } else if (normalized.startsWith("//")) {
            normalized = "https:" + normalized;
        }
        try {
            URI uri = URI.create(normalized);
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || host == null
                    || !(host.equals("mbn.co.kr") || host.endsWith(".mbn.co.kr"))) {
                return null;
            }
            return uri.toString();
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String normalizeJournalist(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String withoutParenthesizedEmail = EMAIL_IN_PARENTHESES.matcher(value).replaceAll(" ");
        String withoutEmail = EMAIL.matcher(withoutParenthesizedEmail).replaceAll("");
        String normalized = withoutEmail.replaceAll("\\s+", " ").trim();
        return normalized.isBlank() ? null : truncate(normalized, 200);
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value.trim()).toInstant();
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private String decode(String value) {
        return HtmlUtils.htmlUnescape(value).trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
