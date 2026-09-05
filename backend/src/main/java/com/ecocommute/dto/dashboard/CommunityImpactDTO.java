package com.ecocommute.dto.dashboard;

import java.util.Map;

public record CommunityImpactDTO(
    double totalCo2SavedTons,
    double totalTreesEquivalent,
    double totalCleanKm,
    long totalEcoTrips,
    long totalActiveUsers,
    Map<String, Long> modalDistribution
) {}
