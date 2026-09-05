package com.ecocommute.dto.admin;

import java.util.Map;

public record AdminDashboardDTO(
    long totalRegisteredUsers,
    long activeUsersCount,
    long totalTripsLogged,
    double totalCo2SavedKg,
    double totalTreesEquivalent,
    long suspiciousTripsCount,
    Map<String, Long> tripsByMode,
    double averageCo2SavedPerTripGrams
) {}
