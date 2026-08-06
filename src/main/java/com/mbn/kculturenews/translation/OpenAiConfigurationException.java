package com.mbn.kculturenews.translation;

public class OpenAiConfigurationException extends RuntimeException {

    public OpenAiConfigurationException() {
        super("OPENAI_API_KEY 환경변수를 설정해 주세요.");
    }
}
