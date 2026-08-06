package com.mbn.kculturenews.culture;

public record CulturalTermResult(
        String term,
        String translatedTerm,
        String romanization,
        String explanation
) {
}
