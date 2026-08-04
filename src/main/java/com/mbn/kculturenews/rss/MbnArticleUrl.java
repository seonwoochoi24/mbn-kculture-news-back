package com.mbn.kculturenews.rss;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class MbnArticleUrl {

    private static final Set<String> TRACKING_PARAMETERS = Set.of(
            "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content"
    );

    private MbnArticleUrl() {
    }

    public static boolean isMbnUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return false;
        }
        try {
            String host = URI.create(rawUrl.trim()).getHost();
            return host != null && isMbnHost(host);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public static String normalize(String rawUrl) {
        try {
            URI parsed = URI.create(rawUrl.trim());
            String host = parsed.getHost();
            if (host == null || !isMbnHost(host)) {
                throw new IllegalArgumentException("MBN 기사 URL이 아닙니다.");
            }

            String query = filterQuery(parsed.getRawQuery());
            URI normalized = new URI(
                    "https",
                    parsed.getRawUserInfo(),
                    host.toLowerCase(Locale.ROOT),
                    -1,
                    normalizePath(parsed.getRawPath()),
                    query,
                    null
            );
            return normalized.toASCIIString();
        } catch (IllegalArgumentException | URISyntaxException exception) {
            throw new IllegalArgumentException("올바르지 않은 MBN 기사 URL입니다: " + rawUrl, exception);
        }
    }

    public static String sha256(String normalizedUrl) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(normalizedUrl.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }

    private static boolean isMbnHost(String host) {
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        return normalizedHost.equals("mbn.co.kr") || normalizedHost.endsWith(".mbn.co.kr");
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        return path.length() > 1 && path.endsWith("/")
                ? path.substring(0, path.length() - 1)
                : path;
    }

    private static String filterQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return null;
        }
        String filtered = Stream.of(rawQuery.split("&"))
                .filter(part -> {
                    String name = part.split("=", 2)[0].toLowerCase(Locale.ROOT);
                    return !TRACKING_PARAMETERS.contains(name);
                })
                .collect(Collectors.joining("&"));
        return filtered.isBlank() ? null : filtered;
    }
}
