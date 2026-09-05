package com.ecocommute.service;

import com.ecocommute.entity.*;
import com.ecocommute.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FraudDetectionAndAdminTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserStatsRepository userStatsRepository;

    @Mock
    private BadgeRepository badgeRepository;

    @Mock
    private UserBadgeRepository userBadgeRepository;

    @Mock
    private CarbonEmissionService carbonEmissionService;

    private GamificationService gamificationService;

    @BeforeEach
    void setUp() {
        gamificationService = new GamificationService(
                tripRepository,
                userRepository,
                userStatsRepository,
                badgeRepository,
                userBadgeRepository,
                carbonEmissionService
        );
    }

    @Test
    @DisplayName("Debe marcar viaje como sospechoso si velocidad de caminata supera umbral (e.g. 15 km en 10 min = 90 km/h)")
    void testSuspiciousWalkingSpeed() {
        User user = new User();
        user.setId("u-123");
        user.setEmail("fraud@ecocommute.org");
        user.setFullName("User Fraud");

        when(userRepository.findById("u-123")).thenReturn(Optional.of(user));
        when(carbonEmissionService.calculateBaselineEmissionGrams(15.0)).thenReturn(2550.0);
        when(carbonEmissionService.calculateModeEmissionGrams(TransportMode.WALKING, 15.0)).thenReturn(0.0);
        when(carbonEmissionService.calculateCo2SavedGrams(TransportMode.WALKING, 15.0)).thenReturn(2550.0);
        when(carbonEmissionService.calculateCaloriesBurned(TransportMode.WALKING, 15.0)).thenReturn(750);
        when(carbonEmissionService.calculatePoints(eq(TransportMode.WALKING), anyDouble(), anyInt())).thenReturn(255);
        when(userStatsRepository.findByUserId("u-123")).thenReturn(Optional.of(new UserStats(user)));
        when(badgeRepository.findAll()).thenReturn(Collections.emptyList());

        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));

        Trip tripInput = new Trip();
        tripInput.setTransportMode(TransportMode.WALKING);
        tripInput.setDistanceKm(15.0);
        tripInput.setDurationMinutes(10); // 90 km/h

        Trip result = gamificationService.recordTrip("u-123", tripInput);

        assertTrue(result.isSuspicious(), "El viaje debe marcarse como sospechoso");
        assertNotNull(result.getSuspiciousReason());
        assertTrue(result.getSuspiciousReason().contains("Velocidad anormal"));
    }
}
