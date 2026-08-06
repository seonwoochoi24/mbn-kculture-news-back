package com.mbn.kculturenews.culture;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "article_cultural_term")
public class ArticleCulturalTerm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long culturalTermId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cultural_analysis_id", nullable = false)
    private ArticleCulturalAnalysis analysis;

    @Column(nullable = false, length = 50)
    private String sourceTerm;

    @Column(length = 100)
    private String translatedTerm;

    @Column(nullable = false, length = 100)
    private String romanization;

    @Column(nullable = false, length = 1000)
    private String explanation;

    @Column(nullable = false)
    private int sortOrder;

    protected ArticleCulturalTerm() {
    }

    static ArticleCulturalTerm of(
            ArticleCulturalAnalysis analysis,
            CulturalTermResult result,
            int sortOrder
    ) {
        ArticleCulturalTerm term = new ArticleCulturalTerm();
        term.analysis = analysis;
        term.sourceTerm = result.term();
        term.translatedTerm = result.translatedTerm();
        term.romanization = result.romanization();
        term.explanation = result.explanation();
        term.sortOrder = sortOrder;
        return term;
    }

    public String getSourceTerm() {
        return sourceTerm;
    }

    public String getTranslatedTerm() {
        return translatedTerm;
    }

    public String getRomanization() {
        return romanization;
    }

    public String getExplanation() {
        return explanation;
    }
}
