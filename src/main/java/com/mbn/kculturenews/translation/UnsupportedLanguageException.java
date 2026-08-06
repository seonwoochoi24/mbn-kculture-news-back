package com.mbn.kculturenews.translation;

public class UnsupportedLanguageException extends RuntimeException {

    public UnsupportedLanguageException(String languageCode) {
        super("지원하지 않는 언어입니다: " + languageCode + ". 지원 언어: ko, en, ja, zh");
    }
}
