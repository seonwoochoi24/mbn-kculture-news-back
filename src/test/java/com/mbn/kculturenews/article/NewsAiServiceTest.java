package com.mbn.kculturenews.article;

import com.mbn.kculturenews.translation.OpenAiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NewsAiServiceTest {

    @Test
    void cachesRepeatedNaturalLanguageKeywordExtraction() {
        TestContext context = context();
        context.server.expect(requestTo("https://api.openai.com/v1/responses"))
                .andRespond(withSuccess(response("{\"keywords\":[\"BTS\",\"그래미\"]}"), MediaType.APPLICATION_JSON));

        assertThat(context.service.extractKeywords("BTS 그래미 관련 기사"))
                .contains(List.of("BTS", "그래미"));
        assertThat(context.service.extractKeywords("BTS 그래미 관련 기사"))
                .contains(List.of("BTS", "그래미"));

        context.server.verify();
    }

    @Test
    void summarizesMultipleArticlesWithOneOpenAiCall() {
        TestContext context = context();
        Article first = article(1L, "첫 기사", "첫 번째 기사 본문");
        Article second = article(2L, "둘째 기사", "두 번째 기사 본문");
        context.server.expect(requestTo("https://api.openai.com/v1/responses"))
                .andRespond(withSuccess(response("""
                        {"summaries":[
                          {"articleId":1,"summary":"첫 기사 요약"},
                          {"articleId":2,"summary":"둘째 기사 요약"}
                        ]}
                        """), MediaType.APPLICATION_JSON));

        context.service.populateSummaries(List.of(first, second));

        verify(first).cacheSummary(org.mockito.ArgumentMatchers.eq("첫 기사 요약"), any(java.time.Instant.class));
        verify(second).cacheSummary(org.mockito.ArgumentMatchers.eq("둘째 기사 요약"), any(java.time.Instant.class));
        verify(context.repository).saveAllAndFlush(anyList());
        context.server.verify();
    }

    private TestContext context() {
        OpenAiProperties properties = new OpenAiProperties();
        properties.setApiKey("test-key");
        ArticleRepository repository = mock(ArticleRepository.class);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NewsAiService service = new NewsAiService(properties, repository, new ObjectMapper(), builder);
        return new TestContext(service, repository, server);
    }

    private Article article(long id, String title, String content) {
        Article article = mock(Article.class);
        when(article.getArticleId()).thenReturn(id);
        when(article.getTitle()).thenReturn(title);
        when(article.getContent()).thenReturn(content);
        when(article.getSummary()).thenReturn(null);
        return article;
    }

    private String response(String outputText) {
        String escaped = outputText.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
        return "{\"output\":[{\"content\":[{\"type\":\"output_text\",\"text\":\"" + escaped + "\"}]}]}";
    }

    private record TestContext(
            NewsAiService service,
            ArticleRepository repository,
            MockRestServiceServer server
    ) {
    }
}
