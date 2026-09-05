package com.ecocommute.dto.dashboard;

public record LeaderboardEntryDTO(
    int rank,
    String userId,
    String fullName,
    String avatarUrl,
    int points,
    double co2SavedKg,
    int tripsCount,
    int streakDays,
    int level
) {}
