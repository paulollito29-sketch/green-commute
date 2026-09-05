package com.ecocommute.service;

import com.ecocommute.entity.Badge;
import com.ecocommute.entity.Trip;
import com.ecocommute.entity.User;
import com.ecocommute.entity.UserStats;
import com.ecocommute.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final UserRepository userRepository;
    private final UserStatsRepository userStatsRepository;
    private final TripRepository tripRepository;
    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;

    public DashboardService(UserRepository userRepository,
                            UserStatsRepository userStatsRepository,
                            TripRepository tripRepository,
                            BadgeRepository badgeRepository,
                            UserBadgeRepository userBadgeRepository) {
        this.userRepository = userRepository;
        this.userStatsRepository = userStatsRepository;
        this.tripRepository = tripRepository;
        this.badgeRepository = badgeRepository;
        this.userBadgeRepository = userBadgeRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUserDashboard(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        UserStats stats = userStatsRepository.findByUserId(userId)
                .orElseGet(() -> new UserStats(user));

        List<Trip> recentTrips = tripRepository.findUserTripsSince(userId, LocalDateTime.now().minusDays(7));

        // Group CO2 saved by day of week
        Map<LocalDate, Double> dailyCo2 = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            dailyCo2.put(today.minusDays(i), 0.0);
        }

        for (Trip trip : recentTrips) {
            LocalDate tripDay = trip.getCompletedAt().toLocalDate();
            if (dailyCo2.containsKey(tripDay)) {
                dailyCo2.put(tripDay, dailyCo2.get(tripDay) + trip.getCo2SavedGrams());
            }
        }

        List<Double> weeklyCo2SavedGrams = new ArrayList<>(dailyCo2.values());
        List<String> weeklyLabels = dailyCo2.keySet().stream()
                .map(d -> d.getDayOfWeek().getDisplayName(TextStyle.SHORT, new Locale("es", "ES")))
                .toList();

        // Trips count by mode
        List<Trip> allUserTrips = tripRepository.findByUserIdOrderByCompletedAtDesc(userId);
        Map<String, Long> tripsByMode = allUserTrips.stream()
                .collect(Collectors.groupingBy(t -> t.getTransportMode().name(), Collectors.counting()));

        // Badges
        List<Badge> unlockedBadges = userBadgeRepository.findByUserId(userId).stream()
                .map(ub -> ub.getBadge())
                .toList();
        List<Badge> allBadges = badgeRepository.findAll();

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("userId", user.getId());
        dashboard.put("fullName", user.getFullName());
        dashboard.put("email", user.getEmail());
        dashboard.put("avatarUrl", user.getAvatarUrl());
        dashboard.put("totalCo2SavedKg", stats.getTotalCo2SavedKg());
        dashboard.put("totalDistanceKm", stats.getTotalDistanceKm());
        dashboard.put("totalTrips", stats.getTotalTrips());
        dashboard.put("currentPoints", user.getCurrentPoints());
        dashboard.put("totalCalories", stats.getTotalCaloriesBurned());
        dashboard.put("treesPlantedEquivalent", stats.getTreesEquivalent());
        dashboard.put("weeklyCo2SavedGrams", weeklyCo2SavedGrams);
        dashboard.put("weeklyLabels", weeklyLabels);
        dashboard.put("tripsByMode", tripsByMode);
        dashboard.put("unlockedBadges", unlockedBadges);
        dashboard.put("allBadges", allBadges);
        return dashboard;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCommunityImpact() {
        double totalCo2 = userStatsRepository.sumTotalCo2SavedKg();
        double totalKm = userStatsRepository.sumTotalDistanceKm();
        long totalTrips = userStatsRepository.sumTotalTrips();
        long activeUsers = userRepository.count();

        Map<String, Object> impact = new HashMap<>();
        impact.put("totalCo2SavedKg", totalCo2);
        impact.put("totalDistanceKm", totalKm);
        impact.put("totalTrips", totalTrips);
        impact.put("totalActiveUsers", activeUsers);
        impact.put("treesPlantedEquivalent", totalCo2 / 21.77);
        return impact;
    }
}
