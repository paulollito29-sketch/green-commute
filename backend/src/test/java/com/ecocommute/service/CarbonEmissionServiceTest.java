package com.ecocommute.service;

import com.ecocommute.entity.EmissionFactor;
import com.ecocommute.entity.TransportMode;
import com.ecocommute.repository.EmissionFactorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarbonEmissionServiceTest {

    @Mock
    private EmissionFactorRepository emissionFactorRepository;

    private CarbonEmissionService emissionService;

    @BeforeEach
    void setUp() {
        emissionService = new CarbonEmissionService(emissionFactorRepository);
    }

    @Test
    @DisplayName("Debe calcular correctamente la emisión de línea base (Auto Particular)")
    void testBaselineCalculation() {
        when(emissionFactorRepository.findByTransportMode(TransportMode.CAR_SOLO))
                .thenReturn(Optional.of(new EmissionFactor(TransportMode.CAR_SOLO, 170.0, "Car Solo")));

        double distanceKm = 10.0;
        double baselineGrams = emissionService.calculateBaselineEmissionGrams(distanceKm);

        assertEquals(1700.0, baselineGrams, 0.001);
    }

    @Test
    @DisplayName("Debe calcular ahorro total de CO2 para bicicleta mecánica (0 g/km)")
    void testBicycleCo2Saved() {
        when(emissionFactorRepository.findByTransportMode(TransportMode.CAR_SOLO))
                .thenReturn(Optional.of(new EmissionFactor(TransportMode.CAR_SOLO, 170.0, "Car Solo")));
        when(emissionFactorRepository.findByTransportMode(TransportMode.BICYCLE))
                .thenReturn(Optional.of(new EmissionFactor(TransportMode.BICYCLE, 0.0, "Bicycle")));

        double distanceKm = 8.0;
        double savedGrams = emissionService.calculateCo2SavedGrams(TransportMode.BICYCLE, distanceKm);

        // 8 * 170 - 8 * 0 = 1360g CO2 saved
        assertEquals(1360.0, savedGrams, 0.001);
    }

    @Test
    @DisplayName("Debe calcular calorías quemadas según modo y distancia")
    void testCaloriesBurned() {
        double distanceKm = 5.0;
        int caloriesWalking = emissionService.calculateCaloriesBurned(TransportMode.WALKING, distanceKm);
        int caloriesBicycle = emissionService.calculateCaloriesBurned(TransportMode.BICYCLE, distanceKm);

        assertEquals(200, caloriesWalking); // 5 * 40
        assertEquals(175, caloriesBicycle); // 5 * 35
    }

    @Test
    @DisplayName("Debe otorgar bono de racha acumulativo en EcoPuntos")
    void testStreakBonusPoints() {
        double co2SavedGrams = 2000.0; // 2 kg saved -> base points = 20
        // Bicycle multiplier = 3.0 -> 20 * 3 = 60 pts
        // 0 days streak -> multiplier = 1.0 -> 60 pts
        int pointsNoStreak = emissionService.calculatePoints(TransportMode.BICYCLE, co2SavedGrams, 0);
        assertEquals(60, pointsNoStreak);

        // 4 days streak -> multiplier = 1 + (4 * 0.05) = 1.20 -> 60 * 1.20 = 72 pts
        int pointsWithStreak = emissionService.calculatePoints(TransportMode.BICYCLE, co2SavedGrams, 4);
        assertEquals(72, pointsWithStreak);
    }

    @Test
    @DisplayName("No debe asignar puntos si el modo no es sostenible o ahorro es <= 0")
    void testNonSustainablePoints() {
        int pointsCar = emissionService.calculatePoints(TransportMode.CAR_SOLO, 0.0, 5);
        assertEquals(0, pointsCar);
    }
}
