package com.mbn.kculturenews.translation;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.openai")
public class OpenAiProperties {

    private String baseUrl = "https://api.openai.com";
    private String apiKey = "";
    private String model = "gpt-4o-mini";
    private String promptVersion = "translation-v1";
    private String culturalPromptVersion = "cultural-term-v1";
    private int maxInputChars = 12000;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public int getMaxInputChars() {
        return maxInputChars;
    }

    public String getCulturalPromptVersion() {
        return culturalPromptVersion;
    }

    public void setCulturalPromptVersion(String culturalPromptVersion) {
        this.culturalPromptVersion = culturalPromptVersion;
    }

    public void setMaxInputChars(int maxInputChars) {
        this.maxInputChars = maxInputChars;
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
