package com.ecocommute.service;

import com.ecocommute.entity.*;
import com.ecocommute.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class GamificationService {

    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final UserStatsRepository userStatsRepository;
    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final CarbonEmissionService carbonEmissionService;

    public GamificationService(TripRepository tripRepository,
                               UserRepository userRepository,
                               UserStatsRepository userStatsRepository,
                               BadgeRepository badgeRepository,
                               UserBadgeRepository userBadgeRepository,
                               CarbonEmissionService carbonEmissionService) {
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
        this.userStatsRepository = userStatsRepository;
        this.badgeRepository = badgeRepository;
        this.userBadgeRepository = userBadgeRepository;
        this.carbonEmissionService = carbonEmissionService;
    }

    @Transactional
    public Trip recordTrip(String userId, Trip tripData) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        double distanceKm = tripData.getDistanceKm();
        TransportMode mode = tripData.getTransportMode() != null ? tripData.getTransportMode() : TransportMode.BICYCLE;

        double baselineCo2 = carbonEmissionService.calculateBaselineEmissionGrams(distanceKm);
        double emittedCo2 = carbonEmissionService.calculateModeEmissionGrams(mode, distanceKm);
        double savedCo2 = carbonEmissionService.calculateCo2SavedGrams(mode, distanceKm);
        int calories = carbonEmissionService.calculateCaloriesBurned(mode, distanceKm);

        // Update Streak
        updateUserStreak(user);

        int points = carbonEmissionService.calculatePoints(mode, savedCo2, user.getStreakDays());

        // Fraud Detection
        int durationMinutes = Math.max(1, tripData.getDurationMinutes());
        double speedKmh = distanceKm / (durationMinutes / 60.0);
        boolean isSuspicious = false;
        String suspiciousReason = null;

        if (mode == TransportMode.WALKING && speedKmh > 12.0) {
            isSuspicious = true;
            suspiciousReason = String.format("Velocidad anormal para caminata: %.1f km/h", speedKmh);
        } else if (mode == TransportMode.BICYCLE && speedKmh > 50.0) {
            isSuspicious = true;
            suspiciousReason = String.format("Velocidad anormal para bicicleta: %.1f km/h", speedKmh);
        }

        Trip trip = new Trip();
        trip.setUser(user);
        trip.setTransportMode(mode);
        trip.setOriginName(tripData.getOriginName() != null ? tripData.getOriginName() : "Punto de Partida");
        trip.setOriginLat(tripData.getOriginLat());
        trip.setOriginLng(tripData.getOriginLng());
        trip.setDestinationName(tripData.getDestinationName() != null ? tripData.getDestinationName() : "Destino");
        trip.setDestinationLat(tripData.getDestinationLat());
        trip.setDestinationLng(tripData.getDestinationLng());
        trip.setDistanceKm(distanceKm);
        trip.setDurationMinutes(durationMinutes);
        trip.setBaselineCo2Grams(baselineCo2);
        trip.setCo2EmittedGrams(emittedCo2);
        trip.setCo2SavedGrams(savedCo2);
        trip.setCaloriesBurned(calories);
        trip.setPointsEarned(points);
        trip.setSuspicious(isSuspicious);
        trip.setSuspiciousReason(suspiciousReason);
        trip.setCompletedAt(LocalDateTime.now());

        trip = tripRepository.save(trip);

        // Update User & Stats
        user.setCurrentPoints(user.getCurrentPoints() + points);
        user.setCurrentLevel(calculateLevel(user.getCurrentPoints()));
        user.setLastTripDate(LocalDateTime.now());
        userRepository.save(user);

        UserStats stats = userStatsRepository.findByUserId(userId)
                .orElseGet(() -> new UserStats(user));
        stats.setTotalCo2SavedKg(stats.getTotalCo2SavedKg() + (savedCo2 / 1000.0));
        stats.setTotalDistanceKm(stats.getTotalDistanceKm() + distanceKm);
        stats.setTotalTrips(stats.getTotalTrips() + 1);
        stats.setTotalCaloriesBurned(stats.getTotalCaloriesBurned() + calories);
        stats.setUpdatedAt(LocalDateTime.now());
        userStatsRepository.save(stats);

        // Check & Unlock Badges
        checkAndAwardBadges(user, stats);

        return trip;
    }

    private void updateUserStreak(User user) {
        LocalDateTime lastTrip = user.getLastTripDate();
        if (lastTrip == null) {
            user.setStreakDays(1);
        } else {
            LocalDate lastTripDay = lastTrip.toLocalDate();
            LocalDate today = LocalDate.now();
            if (lastTripDay.equals(today.minusDays(1))) {
                user.setStreakDays(user.getStreakDays() + 1);
            } else if (!lastTripDay.equals(today)) {
                user.setStreakDays(1);
            }
        }
    }

    private int calculateLevel(int points) {
        if (points < 100) return 1;
        if (points < 300) return 2;
        if (points < 700) return 3;
        if (points < 1500) return 4;
        if (points < 3000) return 5;
        return 6;
    }

    private void checkAndAwardBadges(User user, UserStats stats) {
        List<Badge> allBadges = badgeRepository.findAll();
        for (Badge badge : allBadges) {
            if (!userBadgeRepository.existsByUserIdAndBadgeId(user.getId(), badge.getId())) {
                boolean qualifies = true;
                if (badge.getRequiredPoints() > 0 && user.getCurrentPoints() < badge.getRequiredPoints()) qualifies = false;
                if (badge.getRequiredCo2SavedKg() > 0 && stats.getTotalCo2SavedKg() < badge.getRequiredCo2SavedKg()) qualifies = false;
                if (badge.getRequiredStreakDays() > 0 && user.getStreakDays() < badge.getRequiredStreakDays()) qualifies = false;
                if (badge.getRequiredTrips() > 0 && stats.getTotalTrips() < badge.getRequiredTrips()) qualifies = false;

                if (qualifies) {
                    UserBadge userBadge = new UserBadge(user, badge);
                    userBadgeRepository.save(userBadge);
                }
            }
        }
    }
}
