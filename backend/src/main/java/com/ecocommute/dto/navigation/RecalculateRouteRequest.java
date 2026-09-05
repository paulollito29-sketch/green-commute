package com.ecocommute.dto.navigation;

import java.util.UUID;

public record RecalculateRouteRequest(
        UUID tripId,
        double currentLat,
        double currentLng,
        double destinationLat,
        double destinationLng,
        String vehicleMode,
        double accumulatedCo2SavedGrams,
        double accumulatedDistanceKm
) {}
