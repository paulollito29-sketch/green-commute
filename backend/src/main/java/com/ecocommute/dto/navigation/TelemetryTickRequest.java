package com.ecocommute.dto.navigation;

import java.util.UUID;

public record TelemetryTickRequest(
        UUID tripId,
        double currentLat,
        double currentLng,
        double speedKmh,
        double heading,
        double distanceIncrementMeters,
        long elapsedSeconds
) {}
