package com.ecocommute.dto.auth;

import com.ecocommute.entity.Role;
import java.util.List;

public record UserProfileResponse(
    String id,
    String email,
    String fullName,
    String avatarUrl,
    Role role,
    int currentPoints,
    int currentLevel,
    int streakDays,
    boolean hasBicycle,
    int maxWalkingMinutes,
    double totalCo2SavedKg,
    double totalDistanceKm,
    int totalTrips,
    int totalCaloriesBurned,
    double treesEquivalent,
    List<BadgeSummaryDTO> badges
) {
    public record BadgeSummaryDTO(
        Long id,
        String code,
        String title,
        String description,
        String iconEmoji,
        String awardedAt
    ) {}
}
