package com.ecocommute.dto.trip;

import com.ecocommute.entity.TransportMode;
import java.time.LocalDateTime;

public record TripDTO(
    String id,
    String userId,
    String userFullName,
    TransportMode transportMode,
    String transportModeDisplayName,
    String originName,
    double originLat,
    double originLng,
    String destinationName,
    double destinationLat,
    double destinationLng,
    double distanceKm,
    int durationMinutes,
    double baselineCo2Grams,
    double co2EmittedGrams,
    double co2SavedGrams,
    int caloriesBurned,
    int pointsEarned,
    boolean suspicious,
    String suspiciousReason,
    LocalDateTime completedAt
) {}
