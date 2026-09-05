package com.ecocommute.dto.route;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RoutePlanRequest(
    @NotNull CoordinatesDTO origin,
    @NotNull CoordinatesDTO destination,
    List<String> preferredModes,
    Boolean hasBicycle,
    Integer maxWalkingMinutes,
    String selectedProfile, // "DRIVING", "BICYCLE", "WALKING", "TRANSIT"
    Boolean enableAiOptimization
) {}
