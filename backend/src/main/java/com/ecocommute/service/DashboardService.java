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

        // Weekly trend combined into the shape the dashboard UI expects:
        // [{ dayOfWeek: "lun.", co2SavedGrams: 123.0 }, ...]
        List<Map<String, Object>> weeklyTrend = new ArrayList<>();
        for (int i = 0; i < weeklyLabels.size(); i++) {
            Map<String, Object> day = new HashMap<>();
            day.put("dayOfWeek", weeklyLabels.get(i));
            day.put("co2SavedGrams", weeklyCo2SavedGrams.get(i));
            weeklyTrend.add(day);
        }

        // Trips count by mode
        List<Trip> allUserTrips = tripRepository.findByUserIdOrderByCompletedAtDesc(userId);
        Map<String, Long> tripsByMode = allUserTrips.stream()
                .collect(Collectors.groupingBy(t -> t.getTransportMode().name(), Collectors.counting()));

        // Badges: every badge, marked as unlocked/locked with progress toward the next one
        Set<Long> unlockedBadgeIds = userBadgeRepository.findByUserId(userId).stream()
                .map(ub -> ub.getBadge().getId())
                .collect(Collectors.toSet());
        List<Badge> allBadgeEntities = badgeRepository.findAll();

        List<Map<String, Object>> recentBadges = allBadgeEntities.stream()
                .map(badge -> {
                    boolean unlocked = unlockedBadgeIds.contains(badge.getId());
                    Map<String, Object> b = new HashMap<>();
                    b.put("id", badge.getId());
                    b.put("code", badge.getCode());
                    b.put("title", badge.getTitle());
                    b.put("description", badge.getDescription());
                    b.put("iconEmoji", badge.getIconEmoji());
                    b.put("unlocked", unlocked);
                    b.put("progressPercent", unlocked ? 100 : calculateBadgeProgress(badge, user, stats));
                    return b;
                })
                .toList();

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("userId", user.getId());
        dashboard.put("fullName", user.getFullName());
        dashboard.put("email", user.getEmail());
        dashboard.put("avatarUrl", user.getAvatarUrl());
        dashboard.put("totalCo2SavedKg", stats.getTotalCo2SavedKg());
        dashboard.put("totalDistanceKm", stats.getTotalDistanceKm());
        dashboard.put("totalTrips", stats.getTotalTrips());
        dashboard.put("currentPoints", user.getCurrentPoints());
        dashboard.put("currentLevel", user.getCurrentLevel());
        dashboard.put("totalCaloriesBurned", stats.getTotalCaloriesBurned());
        dashboard.put("treesEquivalent", stats.getTreesEquivalent());
        dashboard.put("weeklyTrend", weeklyTrend);
        dashboard.put("tripsByMode", tripsByMode);
        dashboard.put("recentBadges", recentBadges);
        return dashboard;
    }

    private int calculateBadgeProgress(Badge badge, User user, UserStats stats) {
        List<Double> ratios = new ArrayList<>();
        if (badge.getRequiredPoints() > 0) {
            ratios.add(user.getCurrentPoints() / (double) badge.getRequiredPoints());
        }
        if (badge.getRequiredCo2SavedKg() > 0) {
            ratios.add(stats.getTotalCo2SavedKg() / badge.getRequiredCo2SavedKg());
        }
        if (badge.getRequiredStreakDays() > 0) {
            ratios.add(user.getStreakDays() / (double) badge.getRequiredStreakDays());
        }
        if (badge.getRequiredTrips() > 0) {
            ratios.add(stats.getTotalTrips() / (double) badge.getRequiredTrips());
        }
        if (ratios.isEmpty()) return 0;
        double minRatio = ratios.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        return (int) Math.round(Math.min(1.0, Math.max(0.0, minRatio)) * 100);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCommunityImpact() {
        double totalCo2Kg = userStatsRepository.sumTotalCo2SavedKg();
        double totalKm = userStatsRepository.sumTotalDistanceKm();
        long totalTrips = userStatsRepository.sumTotalTrips();
        long activeUsers = userRepository.count();

        Map<String, Object> impact = new HashMap<>();
        impact.put("totalCo2SavedTons", totalCo2Kg / 1000.0);
        impact.put("totalCleanKm", totalKm);
        impact.put("totalTrips", totalTrips);
        impact.put("totalActiveUsers", activeUsers);
        impact.put("totalTreesEquivalent", totalCo2Kg / 21.77);
        return impact;
    }
}
