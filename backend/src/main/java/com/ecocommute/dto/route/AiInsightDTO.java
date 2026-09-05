package com.ecocommute.dto.route;

public record AiInsightDTO(
    String title,
    String explanation,
    String badgeRecommendation,
    double caloriesEstimated,
    double treesEquivalentFraction,
    String weatherContext
) {}
