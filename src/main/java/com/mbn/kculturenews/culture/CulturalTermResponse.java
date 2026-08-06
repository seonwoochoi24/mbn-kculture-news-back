package com.mbn.kculturenews.culture;

import java.util.List;

public record CulturalTermResponse(
        long articleId,
        String languageCode,
        List<Term> terms
) {
    public static CulturalTermResponse from(long articleId, ArticleCulturalAnalysis analysis) {
        List<Term> terms = analysis.getTerms().stream()
                .map(term -> new Term(
                        term.getTranslatedTerm(),
                        term.getSourceTerm(),
                        term.getRomanization(),
                        term.getExplanation()
                ))
                .toList();
        return new CulturalTermResponse(articleId, analysis.getLanguageCode(), terms);
    }

    public record Term(String term, String sourceTerm, String romanization, String explanation) {
    }
}
