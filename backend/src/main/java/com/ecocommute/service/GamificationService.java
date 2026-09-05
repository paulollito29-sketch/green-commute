package com.ecocommute.service;

import com.ecocommute.entity.*;
import com.ecocommute.dto.trip.TripCreateRequest;
import com.ecocommute.dto.trip.TripDTO;
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
    public TripDTO recordTrip(String userId, TripCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        double baselineCo2 = carbonEmissionService.calculateBaselineEmissionGrams(request.distanceKm());
        double emittedCo2 = carbonEmissionService.calculateModeEmissionGrams(request.transportMode(), request.distanceKm());
        double savedCo2 = carbonEmissionService.calculateCo2SavedGrams(request.transportMode(), request.distanceKm());
        int calories = carbonEmissionService.calculateCaloriesBurned(request.transportMode(), request.distanceKm());

        // Update Streak
        updateUserStreak(user);

        int points = carbonEmissionService.calculatePoints(request.transportMode(), savedCo2, user.getStreakDays());

        // Fraud & Suspicious Activity Detection
        double durationHours = Math.max(0.01, request.durationMinutes() / 60.0);
        double speedKmh = request.distanceKm() / durationHours;
        boolean isSuspicious = false;
        String suspiciousReason = null;

        if (request.transportMode() == TransportMode.WALKING && speedKmh > 12.0) {
            isSuspicious = true;
            suspiciousReason = String.format("Velocidad anormal para caminata: %.1f km/h", speedKmh);
        } else if (request.transportMode() == TransportMode.BICYCLE && speedKmh > 50.0) {
            isSuspicious = true;
            suspiciousReason = String.format("Velocidad anormal para bicicleta: %.1f km/h", speedKmh);
        }

        Trip trip = new Trip();
        trip.setUser(user);
        trip.setTransportMode(request.transportMode());
        trip.setOriginName(request.originName() != null ? request.originName() : "Punto de Partida");
        trip.setOriginLat(request.originLat());
        trip.setOriginLng(request.originLng());
        trip.setDestinationName(request.destinationName() != null ? request.destinationName() : "Destino");
        trip.setDestinationLat(request.destinationLat());
        trip.setDestinationLng(request.destinationLng());
        trip.setDistanceKm(request.distanceKm());
        trip.setDurationMinutes(request.durationMinutes());
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
        stats.setTotalDistanceKm(stats.getTotalDistanceKm() + request.distanceKm());
        stats.setTotalTrips(stats.getTotalTrips() + 1);
        stats.setTotalCaloriesBurned(stats.getTotalCaloriesBurned() + calories);
        stats.setUpdatedAt(LocalDateTime.now());
        userStatsRepository.save(stats);

        // Check & Unlock Badges
        checkAndAwardBadges(user, stats);

        return toTripDTO(trip);
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
                user.setStreakDays(1); // Reset streak if missed a day
            }
        }
    }

    private int calculateLevel(int points) {
        if (points < 100) return 1; // Semilla Verde
        if (points < 300) return 2; // Brote Urbano
        if (points < 700) return 3; // Árbol Sostenible
        if (points < 1500) return 4; // Guardián del Aire
        if (points < 3000) return 5; // Héroe Climático
        return 6; // Maestro del Ecosistema
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

    private TripDTO toTripDTO(Trip trip) {
        return new TripDTO(
                trip.getId(),
                trip.getUser().getId(),
                trip.getUser().getFullName(),
                trip.getTransportMode(),
                trip.getTransportMode().getDisplayName(),
                trip.getOriginName(),
                trip.getOriginLat(),
                trip.getOriginLng(),
                trip.getDestinationName(),
                trip.getDestinationLat(),
                trip.getDestinationLng(),
                trip.getDistanceKm(),
                trip.getDurationMinutes(),
                trip.getBaselineCo2Grams(),
                trip.getCo2EmittedGrams(),
                trip.getCo2SavedGrams(),
                trip.getCaloriesBurned(),
                trip.getPointsEarned(),
                trip.isSuspicious(),
                trip.getSuspiciousReason(),
                trip.getCompletedAt()
        );
    }
}
