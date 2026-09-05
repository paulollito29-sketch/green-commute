package com.ecocommute.dto.admin;

import com.ecocommute.entity.TransportMode;
import java.time.LocalDateTime;

public record AdminTripAuditDTO(
    String id,
    String userId,
    String userFullName,
    String userEmail,
    TransportMode transportMode,
    String originName,
    String destinationName,
    double distanceKm,
    int durationMinutes,
    double speedKmh,
    double co2SavedGrams,
    int pointsEarned,
    boolean suspicious,
    String suspiciousReason,
    LocalDateTime completedAt
) {}
