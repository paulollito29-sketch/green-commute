package com.ecocommute.dto.navigation;

import java.util.List;
import java.util.UUID;

public record EcoRouteResponse(
        UUID tripId,
        String vehicleMode,
        double totalDistanceKm,
        int totalDurationSeconds,
        double baselineCo2Grams,
        double ecoCo2Grams,
        double estimatedCo2SavedGrams,
        List<List<Double>> pathCoordinates,
        List<ManeuverStepDTO> maneuvers,
        String greenInsight
) {}
