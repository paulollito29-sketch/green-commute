package com.ecocommute.dto.route;

import com.ecocommute.entity.TransportMode;
import java.util.List;

public record RouteOptionDTO(
    String id,
    String title,
    TransportMode mode,
    String modeDisplayName,
    double distanceKm,
    int durationMinutes,
    double co2EmittedGrams,
    double co2SavedGrams,
    int potentialPoints,
    int caloriesBurned,
    boolean isAiRecommended,
    AiInsightDTO aiInsight,
    List<List<Double>> pathCoordinates, // [[lat, lng], [lat, lng]...]
    String summary
) {}
