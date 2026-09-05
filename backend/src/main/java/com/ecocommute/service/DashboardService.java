package com.ecocommute.service;

import com.ecocommute.entity.Badge;
import com.ecocommute.entity.Trip;
import com.ecocommute.entity.User;
import com.ecocommute.entity.UserStats;
import com.ecocommute.dto.dashboard.CommunityImpactDTO;
import com.ecocommute.dto.dashboard.UserDashboardDTO;
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
    public UserDashboardDTO getUserDashboard(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        UserStats stats = userStatsRepository.findByUserId(userId)
                .orElseGet(() -> new UserStats(user));

        // Weekly Trend (Past 7 days)
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(6).withHour(0).withMinute(0).withSecond(0);
        List<Trip> recentTrips = tripRepository.findUserTripsSince(userId, sevenDaysAgo);

        Map<LocalDate, List<Trip>> tripsByDate = recentTrips.stream()
                .collect(Collectors.groupingBy(t -> t.getCompletedAt().toLocalDate()));

        List<UserDashboardDTO.DailyTrendDTO> weeklyTrend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            List<Trip> dayTrips = tripsByDate.getOrDefault(date, Collections.emptyList());

            double dayCo2 = dayTrips.stream().mapToDouble(Trip::getCo2SavedGrams).sum();
            double dayDist = dayTrips.stream().mapToDouble(Trip::getDistanceKm).sum();
            int dayPoints = dayTrips.stream().mapToInt(Trip::getPointsEarned).sum();

            String dayName = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, new Locale("es", "ES"));
            weeklyTrend.add(new UserDashboardDTO.DailyTrendDTO(
                    dayName.substring(0, 1).toUpperCase() + dayName.substring(1),
                    date.toString(),
                    Math.round(dayCo2 * 10.0) / 10.0,
                    Math.round(dayDist * 10.0) / 10.0,
                    dayPoints
            ));
        }

        // Trips by mode
        List<Trip> allUserTrips = tripRepository.findByUserIdOrderByCompletedAtDesc(userId);
        Map<String, Long> tripsByMode = allUserTrips.stream()
                .collect(Collectors.groupingBy(t -> t.getTransportMode().getDisplayName(), Collectors.counting()));

        // Badges progress
        Set<Long> unlockedBadgeIds = userBadgeRepository.findByUserId(userId).stream()
                .map(ub -> ub.getBadge().getId())
                .collect(Collectors.toSet());

        List<UserDashboardDTO.BadgeProgressDTO> recentBadges = badgeRepository.findAll().stream()
                .map(b -> {
                    boolean unlocked = unlockedBadgeIds.contains(b.getId());
                    int progress = unlocked ? 100 : calculateBadgeProgress(b, user, stats);
                    return new UserDashboardDTO.BadgeProgressDTO(
                            b.getId(),
                            b.getCode(),
                            b.getTitle(),
                            b.getDescription(),
                            b.getIconEmoji(),
                            unlocked,
                            progress
                    );
                })
                .toList();

        return new UserDashboardDTO(
                Math.round(stats.getTotalCo2SavedKg() * 10.0) / 10.0,
                Math.round(stats.getTreesEquivalent() * 100.0) / 100.0,
                Math.round(stats.getTotalDistanceKm() * 10.0) / 10.0,
                stats.getTotalTrips(),
                stats.getTotalCaloriesBurned(),
                user.getCurrentPoints(),
                user.getCurrentLevel(),
                user.getStreakDays(),
                weeklyTrend,
                tripsByMode,
                recentBadges
        );
    }

    private int calculateBadgeProgress(Badge b, User user, UserStats stats) {
        double p = 0;
        if (b.getRequiredPoints() > 0) {
            p = Math.max(p, (double) user.getCurrentPoints() / b.getRequiredPoints());
        }
        if (b.getRequiredCo2SavedKg() > 0) {
            p = Math.max(p, stats.getTotalCo2SavedKg() / b.getRequiredCo2SavedKg());
        }
        if (b.getRequiredStreakDays() > 0) {
            p = Math.max(p, (double) user.getStreakDays() / b.getRequiredStreakDays());
        }
        if (b.getRequiredTrips() > 0) {
            p = Math.max(p, (double) stats.getTotalTrips() / b.getRequiredTrips());
        }
        return (int) Math.min(99, Math.round(p * 100.0));
    }

    @Transactional(readOnly = true)
    public CommunityImpactDTO getCommunityImpact() {
        double totalCo2Kg = userStatsRepository.sumTotalCo2SavedKg();
        double totalKm = userStatsRepository.sumTotalDistanceKm();
        long totalTrips = userStatsRepository.sumTotalTrips();
        long activeUsers = userRepository.count();

        Map<String, Long> modalDistribution = new HashMap<>();
        List<Object[]> modalCounts = tripRepository.countTripsByTransportMode();
        for (Object[] row : modalCounts) {
            if (row[0] != null) {
                modalDistribution.put(row[0].toString(), (Long) row[1]);
            }
        }

        return new CommunityImpactDTO(
                Math.round((totalCo2Kg / 1000.0) * 100.0) / 100.0, // in Metric Tons
                Math.round((totalCo2Kg / 22.0) * 10.0) / 10.0,
                Math.round(totalKm * 10.0) / 10.0,
                totalTrips,
                activeUsers,
                modalDistribution
        );
    }
}
