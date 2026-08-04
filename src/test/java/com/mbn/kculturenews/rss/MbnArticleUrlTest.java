package com.mbn.kculturenews.rss;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MbnArticleUrlTest {

    @Test
    void normalizesMbnUrlAndRemovesTrackingParameters() {
        String normalized = MbnArticleUrl.normalize(
                "http://m.mbn.co.kr/news/123/?utm_source=test&article=1#section"
        );

        assertThat(normalized).isEqualTo("https://m.mbn.co.kr/news/123?article=1");
    }

    @Test
    void rejectsNonMbnDomain() {
        assertThatThrownBy(() -> MbnArticleUrl.normalize("https://fake-mbn.co.kr/news/1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void identifiesMbnSubdomainsOnly() {
        assertThat(MbnArticleUrl.isMbnUrl("https://m.mbn.co.kr/news/1")).isTrue();
        assertThat(MbnArticleUrl.isMbnUrl("https://fake-mbn.co.kr/news/1")).isFalse();
        assertThat(MbnArticleUrl.isMbnUrl("https://example.com/?url=mbn.co.kr")).isFalse();
    }

    @Test
    void createsStableSha256Hash() {
        String normalized = "https://www.mbn.co.kr/news/123";

        assertThat(MbnArticleUrl.sha256(normalized))
                .hasSize(64)
                .isEqualTo(MbnArticleUrl.sha256(normalized));
    }
}
