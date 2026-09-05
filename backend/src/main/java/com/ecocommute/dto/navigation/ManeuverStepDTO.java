package com.ecocommute.dto.navigation;

import java.util.List;

public record ManeuverStepDTO(
        String instruction,
        String maneuverType, // "START", "TURN_RIGHT", "TURN_LEFT", "CONTINUE", "ARRIVE"
        double distanceMeters,
        int durationSeconds,
        List<Double> location
) {}
