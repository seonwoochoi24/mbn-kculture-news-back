package com.mbn.kculturenews.culture;

public class UnsupportedCulturalTermLanguageException extends RuntimeException {

    public UnsupportedCulturalTermLanguageException(String languageCode) {
        super("문화 용어 설명 언어는 en, ja, zh 중 하나여야 합니다: " + languageCode);
    }
}
