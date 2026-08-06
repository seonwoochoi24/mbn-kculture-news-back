package com.mbn.kculturenews.culture;

import com.mbn.kculturenews.translation.SupportedLanguage;

import java.util.List;

public interface CulturalTermClient {

    List<CulturalTermResult> extract(
            String title,
            String content,
            SupportedLanguage targetLanguage
    );
}
