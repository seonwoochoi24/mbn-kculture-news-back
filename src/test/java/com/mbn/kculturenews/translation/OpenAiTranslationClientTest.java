package com.mbn.kculturenews.translation;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenAiTranslationClientTest {

    @Test
    void translatesWithStructuredResponseAndDoesNotExposeApiKeyInBody() {
        OpenAiProperties properties = properties("test-openai-key");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiTranslationClient client = new OpenAiTranslationClient(
                properties,
                new ObjectMapper(),
                builder
        );

        server.expect(requestTo("https://api.openai.com/v1/responses"))
                .andExpect(header("Authorization", "Bearer test-openai-key"))
                .andExpect(content().string(containsString("\"model\":\"gpt-4o-mini\"")))
                .andExpect(content().string(containsString("\"type\":\"json_schema\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("test-openai-key"))))
                .andRespond(withSuccess("""
                        {
                          "status": "completed",
                          "output": [
                            {
                              "type": "message",
                              "content": [
                                {
                                  "type": "output_text",
                                  "text": "{\\\"title\\\":\\\"Translated title\\\",\\\"content\\\":\\\"Translated content\\\",\\\"summary\\\":\\\"Translated summary\\\"}"
                                }
                              ]
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        TranslationResult result = client.translate(
                "한국어 제목",
                "한국어 설명",
                "한국어 요약",
                SupportedLanguage.ENGLISH
        );

        assertThat(result.title()).isEqualTo("Translated title");
        assertThat(result.content()).isEqualTo("Translated content");
        assertThat(result.summary()).isEqualTo("Translated summary");
        server.verify();
    }

    @Test
    void rejectsTranslationWhenApiKeyIsMissing() {
        OpenAiTranslationClient client = new OpenAiTranslationClient(
                properties(""),
                new ObjectMapper(),
                RestClient.builder()
        );

        assertThatThrownBy(() -> client.translate("제목", "설명", "요약", SupportedLanguage.ENGLISH))
                .isInstanceOf(OpenAiConfigurationException.class);
    }

    private OpenAiProperties properties(String apiKey) {
        OpenAiProperties properties = new OpenAiProperties();
        properties.setApiKey(apiKey);
        return properties;
    }
}
