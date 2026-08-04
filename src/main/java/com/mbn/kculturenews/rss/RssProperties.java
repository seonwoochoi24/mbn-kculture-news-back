package com.mbn.kculturenews.rss;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@ConfigurationProperties(prefix = "app.rss")
public class RssProperties {

    private boolean enabled = true;
    private URI entertainmentUrl = URI.create("https://www.mbn.co.kr/rss/enter/");
    private long intervalMs = 300_000;
    private long initialDelayMs = 10_000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public URI getEntertainmentUrl() {
        return entertainmentUrl;
    }

    public void setEntertainmentUrl(URI entertainmentUrl) {
        this.entertainmentUrl = entertainmentUrl;
    }

    public long getIntervalMs() {
        return intervalMs;
    }

    public void setIntervalMs(long intervalMs) {
        this.intervalMs = intervalMs;
    }

    public long getInitialDelayMs() {
        return initialDelayMs;
    }

    public void setInitialDelayMs(long initialDelayMs) {
        this.initialDelayMs = initialDelayMs;
    }
}
