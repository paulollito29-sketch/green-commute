package com.ecocommute.dto.navigation;

public record EcoRouteRequest(
        double originLat,
        double originLng,
        double destinationLat,
        double destinationLng,
        String vehicleMode // "BICYCLE", "WALKING", "DRIVING"
) {}
