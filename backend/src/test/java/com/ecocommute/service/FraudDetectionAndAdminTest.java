package com.ecocommute.service;

import com.ecocommute.entity.*;
import com.ecocommute.dto.trip.TripCreateRequest;
import com.ecocommute.dto.trip.TripDTO;
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
    @DisplayName("Debe detectar y marcar como sospechoso un viaje a pie con velocidad imposible (> 12 km/h)")
    void testFraudDetectionWalkingSpeedAnomaly() {
        User user = new User("trampa@ecocommute.org", "pass", "Usuario Trampa", Role.ROLE_USER);
        user.setId("u-123");

        when(userRepository.findById("u-123")).thenReturn(Optional.of(user));
        when(carbonEmissionService.calculateBaselineEmissionGrams(anyDouble())).thenReturn(2000.0);
        when(carbonEmissionService.calculateModeEmissionGrams(any(), anyDouble())).thenReturn(0.0);
        when(carbonEmissionService.calculateCo2SavedGrams(any(), anyDouble())).thenReturn(2000.0);
        when(carbonEmissionService.calculateCaloriesBurned(any(), anyDouble())).thenReturn(400);
        when(carbonEmissionService.calculatePoints(any(), anyDouble(), anyInt())).thenReturn(50);
        when(userStatsRepository.findByUserId("u-123")).thenReturn(Optional.of(new UserStats(user)));
        when(badgeRepository.findAll()).thenReturn(Collections.emptyList());

        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> {
            Trip t = inv.getArgument(0);
            t.setId("trip-fraud-1");
            return t;
        });

        // 10 km walking in 10 minutes = 60 km/h (Physically impossible walking)
        TripCreateRequest req = new TripCreateRequest(
                TransportMode.WALKING, "Origen", -12.0, -77.0, "Destino", -12.1, -77.1, 10.0, 10
        );

        TripDTO recorded = gamificationService.recordTrip("u-123", req);

        assertNotNull(recorded);
        assertTrue(recorded.suspicious(), "El viaje debe ser marcado como sospechoso por el motor anti-fraude");
        assertNotNull(recorded.suspiciousReason());
        assertTrue(recorded.suspiciousReason().contains("Velocidad anormal"));
    }
}
