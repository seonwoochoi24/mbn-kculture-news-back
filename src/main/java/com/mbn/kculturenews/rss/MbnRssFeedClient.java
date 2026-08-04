package com.mbn.kculturenews.rss;

import com.mbn.kculturenews.common.RssCollectionException;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Component
public class MbnRssFeedClient implements RssFeedClient {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    public List<RssItem> fetch(URI feedUri) {
        HttpRequest request = HttpRequest.newBuilder(feedUri)
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/rss+xml, application/xml, text/xml")
                .header("User-Agent", "MBN-Knews-RssCollector/1.0")
                .GET()
                .build();

        try {
            HttpResponse<InputStream> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofInputStream()
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                response.body().close();
                throw new RssCollectionException(
                        "MBN RSS가 HTTP " + response.statusCode() + "을 반환했습니다.",
                        null
                );
            }

            try (InputStream body = response.body(); XmlReader reader = new XmlReader(body)) {
                SyndFeed feed = new SyndFeedInput().build(reader);
                return feed.getEntries().stream().map(this::toItem).toList();
            }
        } catch (RssCollectionException exception) {
            throw exception;
        } catch (Exception exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RssCollectionException("MBN 연예 RSS를 가져오지 못했습니다.", exception);
        }
    }

    private RssItem toItem(SyndEntry entry) {
        String description = entry.getDescription() == null
                ? null
                : cleanText(entry.getDescription().getValue());
        return new RssItem(
                blankToNull(entry.getUri()),
                entry.getLink(),
                cleanText(entry.getTitle()),
                description,
                entry.getPublishedDate() == null ? null : entry.getPublishedDate().toInstant()
        );
    }

    private String cleanText(String value) {
        if (value == null) {
            return null;
        }
        String withoutTags = value.replaceAll("<[^>]+>", " ");
        return HtmlUtils.htmlUnescape(withoutTags).replaceAll("\\s+", " ").trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
