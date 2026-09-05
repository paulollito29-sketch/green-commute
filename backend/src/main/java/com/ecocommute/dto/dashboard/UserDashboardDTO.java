package com.ecocommute.dto.dashboard;

import java.util.List;
import java.util.Map;

public record UserDashboardDTO(
    double totalCo2SavedKg,
    double treesEquivalent,
    double totalDistanceKm,
    int totalTrips,
    int totalCaloriesBurned,
    int currentPoints,
    int currentLevel,
    int streakDays,
    List<DailyTrendDTO> weeklyTrend,
    Map<String, Long> tripsByMode,
    List<BadgeProgressDTO> recentBadges
) {
    public record DailyTrendDTO(
        String dayOfWeek,
        String date,
        double co2SavedGrams,
        double distanceKm,
        int pointsEarned
    ) {}

    public record BadgeProgressDTO(
        Long id,
        String code,
        String title,
        String description,
        String iconEmoji,
        boolean unlocked,
        int progressPercent
    ) {}
}
