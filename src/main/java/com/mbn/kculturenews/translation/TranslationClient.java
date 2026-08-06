package com.mbn.kculturenews.translation;

public interface TranslationClient {

    TranslationResult translate(
            String title,
            String content,
            String summary,
            SupportedLanguage targetLanguage
    );
}
