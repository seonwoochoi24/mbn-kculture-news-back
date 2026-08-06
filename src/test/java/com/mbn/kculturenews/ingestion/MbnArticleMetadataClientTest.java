package com.mbn.kculturenews.ingestion;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class MbnArticleMetadataClientTest {

    private final MbnArticleMetadataClient client = new MbnArticleMetadataClient();

    @Test
    void extractsImageJournalistAndPublishedDateFromMbnMetaTags() {
        String html = """
                <html><head>
                  <meta content="2026-03-08T10:08:00+09:00" property="article:published_time">
                  <meta property="article:author" content="박소진 MK스포츠 기자(psj23@mkculture.com)">
                  <meta data-rh="true" property="og:image"
                        content="http://img.mbn.co.kr/filewww/news/other/article.jpg">
                </head><body>
                  <div class="detail" id="newsViewArea" itemprop="articleBody">
                    <p>첫 번째 원문 문단입니다. 기사 본문을 정확히 저장하기 위한 테스트 문장입니다.<br><br>
                    두 번째 원문 문단입니다. 충분한 길이의 기사 본문이며 요약문이 아닌 전체 내용을 나타냅니다.<br>
                    [MBN스타 박소진 기자 mkculture@mkculture.com]</p>
                  </div>
                  <!--// 기사 본문 내용 -->
                </body></html>
                """;

        MbnArticleMetadata metadata = client.parse(html);

        assertThat(metadata.imageUrl())
                .isEqualTo("https://img.mbn.co.kr/filewww/news/other/article.jpg");
        assertThat(metadata.journalistName()).isEqualTo("박소진 MK스포츠 기자");
        assertThat(metadata.publishedAt()).isEqualTo(Instant.parse("2026-03-08T01:08:00Z"));
        assertThat(metadata.content())
                .contains("첫 번째 원문 문단입니다.", "두 번째 원문 문단입니다.")
                .doesNotContain("mkculture.com");
    }

    @Test
    void rejectsAnExternalImageHost() {
        MbnArticleMetadata metadata = client.parse(
                "<meta property=\"og:image\" content=\"https://evil.example/image.jpg\">"
        );

        assertThat(metadata.imageUrl()).isNull();
    }

    @Test
    void removesVodPlayerInstructionsFromArticleContent() {
        String html = """
                <div class="detail" id="newsViewArea" itemprop="articleBody">
                  VOD 시청 안내<br>
                  어도비 플래시 플레이어 서비스 종료에 따라<br>
                  브라우저 업그레이드 및 설치<br>
                  실제 뉴스 원문 첫 문장입니다. 방송에서 전달한 핵심 기사 내용입니다.<br>
                  실제 뉴스 원문 두 번째 문장입니다. 충분한 길이의 원문을 구성합니다.
                </div>
                <!--// 기사 본문 내용 -->
                """;

        MbnArticleMetadata metadata = client.parse(html);

        assertThat(metadata.content())
                .contains("실제 뉴스 원문 첫 문장입니다.")
                .doesNotContain("VOD 시청 안내", "플래시 플레이어", "브라우저 업그레이드");
    }
}
