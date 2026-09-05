package com.ecocommute.service;

import com.ecocommute.entity.*;
import com.ecocommute.dto.admin.*;
import com.ecocommute.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final UserStatsRepository userStatsRepository;
    private final TripRepository tripRepository;
    private final EmissionFactorRepository emissionFactorRepository;

    public AdminService(UserRepository userRepository,
                        UserStatsRepository userStatsRepository,
                        TripRepository tripRepository,
                        EmissionFactorRepository emissionFactorRepository) {
        this.userRepository = userRepository;
        this.userStatsRepository = userStatsRepository;
        this.tripRepository = tripRepository;
        this.emissionFactorRepository = emissionFactorRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardDTO getAdminDashboardKPIs() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.findAll().stream().filter(User::isActive).count();
        long totalTrips = tripRepository.count();
        double totalCo2Kg = userStatsRepository.sumTotalCo2SavedKg();
        double totalTrees = totalCo2Kg / 22.0;

        long suspiciousCount = tripRepository.findBySuspiciousTrueOrderByCompletedAtDesc(PageRequest.of(0, 1)).getTotalElements();

        Map<String, Long> tripsByMode = new HashMap<>();
        for (Object[] row : tripRepository.countTripsByTransportMode()) {
            if (row[0] != null) {
                tripsByMode.put(row[0].toString(), (Long) row[1]);
            }
        }

        double avgCo2PerTrip = totalTrips > 0 ? (totalCo2Kg * 1000.0) / totalTrips : 0.0;

        return new AdminDashboardDTO(
                totalUsers,
                activeUsers,
                totalTrips,
                Math.round(totalCo2Kg * 10.0) / 10.0,
                Math.round(totalTrees * 10.0) / 10.0,
                suspiciousCount,
                tripsByMode,
                Math.round(avgCo2PerTrip * 10.0) / 10.0
        );
    }

    @Transactional(readOnly = true)
    public Page<AdminUserDTO> getUsers(int page, int size, String search) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> usersPage;

        if (search != null && !search.isBlank()) {
            usersPage = userRepository.findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(search, search, pageRequest);
        } else {
            usersPage = userRepository.findAll(pageRequest);
        }

        return usersPage.map(u -> {
            UserStats stats = userStatsRepository.findByUserId(u.getId()).orElse(null);
            double co2Saved = stats != null ? stats.getTotalCo2SavedKg() : 0.0;
            int trips = stats != null ? stats.getTotalTrips() : 0;
            return new AdminUserDTO(
                    u.getId(),
                    u.getEmail(),
                    u.getFullName(),
                    u.getAvatarUrl(),
                    u.getRole(),
                    u.isActive(),
                    u.getAuthProvider(),
                    u.getCurrentPoints(),
                    u.getCurrentLevel(),
                    u.getStreakDays(),
                    Math.round(co2Saved * 10.0) / 10.0,
                    trips,
                    u.getCreatedAt(),
                    u.getLastTripDate()
            );
        });
    }

    @Transactional
    public void updateUserStatus(String userId, boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        user.setActive(active);
        userRepository.save(user);
    }

    @Transactional
    public void updateUserRole(String userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        user.setRole(role);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Page<AdminTripAuditDTO> getTripsForAudit(int page, int size, boolean suspiciousOnly) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "completedAt"));
        Page<Trip> trips;

        if (suspiciousOnly) {
            trips = tripRepository.findBySuspiciousTrueOrderByCompletedAtDesc(pageRequest);
        } else {
            trips = tripRepository.findAll(pageRequest);
        }

        return trips.map(t -> {
            double durationHours = Math.max(0.01, t.getDurationMinutes() / 60.0);
            double speed = t.getDistanceKm() / durationHours;
            return new AdminTripAuditDTO(
                    t.getId(),
                    t.getUser().getId(),
                    t.getUser().getFullName(),
                    t.getUser().getEmail(),
                    t.getTransportMode(),
                    t.getOriginName(),
                    t.getDestinationName(),
                    t.getDistanceKm(),
                    t.getDurationMinutes(),
                    Math.round(speed * 10.0) / 10.0,
                    t.getCo2SavedGrams(),
                    t.getPointsEarned(),
                    t.isSuspicious(),
                    t.getSuspiciousReason(),
                    t.getCompletedAt()
            );
        });
    }

    @Transactional
    public void deleteTripAndRevertStats(String tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Viaje no encontrado"));

        User user = trip.getUser();
        UserStats stats = userStatsRepository.findByUserId(user.getId()).orElse(null);

        // Revert user points
        user.setCurrentPoints(Math.max(0, user.getCurrentPoints() - trip.getPointsEarned()));
        userRepository.save(user);

        if (stats != null) {
            stats.setTotalCo2SavedKg(Math.max(0.0, stats.getTotalCo2SavedKg() - (trip.getCo2SavedGrams() / 1000.0)));
            stats.setTotalDistanceKm(Math.max(0.0, stats.getTotalDistanceKm() - trip.getDistanceKm()));
            stats.setTotalTrips(Math.max(0, stats.getTotalTrips() - 1));
            stats.setTotalCaloriesBurned(Math.max(0, stats.getTotalCaloriesBurned() - trip.getCaloriesBurned()));
            userStatsRepository.save(stats);
        }

        tripRepository.delete(trip);
    }

    @Transactional(readOnly = true)
    public List<EmissionFactorDTO> getEmissionFactors() {
        return emissionFactorRepository.findAll().stream()
                .map(ef -> new EmissionFactorDTO(
                        ef.getId(),
                        ef.getTransportMode(),
                        ef.getTransportMode().getDisplayName(),
                        ef.getGramsCo2PerKm(),
                        ef.getDescription()
                ))
                .toList();
    }

    @Transactional
    public EmissionFactorDTO updateEmissionFactor(Long id, double gramsCo2PerKm) {
        EmissionFactor ef = emissionFactorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Factor no encontrado"));

        ef.setGramsCo2PerKm(gramsCo2PerKm);
        ef.setUpdatedAt(LocalDateTime.now());
        ef = emissionFactorRepository.save(ef);

        return new EmissionFactorDTO(
                ef.getId(),
                ef.getTransportMode(),
                ef.getTransportMode().getDisplayName(),
                ef.getGramsCo2PerKm(),
                ef.getDescription()
        );
    }
}
