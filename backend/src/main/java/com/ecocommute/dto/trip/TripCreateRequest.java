package com.ecocommute.dto.trip;

import com.ecocommute.entity.TransportMode;
import jakarta.validation.constraints.NotNull;

public record TripCreateRequest(
    @NotNull TransportMode transportMode,
    String originName,
    double originLat,
    double originLng,
    String destinationName,
    double destinationLat,
    double destinationLng,
    double distanceKm,
    int durationMinutes
) {}
