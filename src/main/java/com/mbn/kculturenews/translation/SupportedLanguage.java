package com.mbn.kculturenews.translation;

import java.util.Arrays;
import java.util.Locale;

public enum SupportedLanguage {
    KOREAN("ko", "Korean"),
    ENGLISH("en", "English"),
    JAPANESE("ja", "Japanese"),
    CHINESE("zh", "Simplified Chinese");

    private final String code;
    private final String displayName;

    SupportedLanguage(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static SupportedLanguage fromCode(String value) {
        String normalized = value == null ? "ko" : value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(language -> language.code.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new UnsupportedLanguageException(normalized));
    }
}
