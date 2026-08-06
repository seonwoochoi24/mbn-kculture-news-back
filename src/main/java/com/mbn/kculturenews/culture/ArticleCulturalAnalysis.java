package com.mbn.kculturenews.culture;

import com.mbn.kculturenews.article.Article;
import com.mbn.kculturenews.translation.SupportedLanguage;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "article_cultural_analysis")
public class ArticleCulturalAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long culturalAnalysisId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Column(nullable = false, length = 10)
    private String languageCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CulturalAnalysisStatus analysisStatus;

    @Column(nullable = false, length = 100)
    private String modelName;

    @Column(nullable = false, length = 50)
    private String promptVersion;

    @Column(nullable = false)
    private Instant generatedAt;

    @OrderBy("sortOrder ASC")
    @OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ArticleCulturalTerm> terms = new ArrayList<>();

    protected ArticleCulturalAnalysis() {
    }

    public static ArticleCulturalAnalysis completed(
            Article article,
            SupportedLanguage language,
            List<CulturalTermResult> results,
            String modelName,
            String promptVersion,
            Instant generatedAt
    ) {
        ArticleCulturalAnalysis analysis = new ArticleCulturalAnalysis();
        analysis.article = article;
        analysis.languageCode = language.getCode();
        analysis.analysisStatus = CulturalAnalysisStatus.COMPLETED;
        analysis.modelName = modelName;
        analysis.promptVersion = promptVersion;
        analysis.generatedAt = generatedAt;
        for (int index = 0; index < results.size(); index++) {
            analysis.terms.add(ArticleCulturalTerm.of(analysis, results.get(index), index));
        }
        return analysis;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public List<ArticleCulturalTerm> getTerms() {
        return List.copyOf(terms);
    }
}
