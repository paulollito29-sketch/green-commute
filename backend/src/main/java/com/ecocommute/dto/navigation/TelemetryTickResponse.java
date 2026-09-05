package com.ecocommute.dto.navigation;

public record TelemetryTickResponse(
        boolean success,
        double totalCo2SavedGrams,
        double remainingDistanceKm,
        boolean requiresRecalculation
) {}
