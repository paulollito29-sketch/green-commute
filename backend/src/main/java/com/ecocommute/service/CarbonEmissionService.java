package com.ecocommute.service;

import com.ecocommute.entity.EmissionFactor;
import com.ecocommute.entity.TransportMode;
import com.ecocommute.repository.EmissionFactorRepository;
import org.springframework.stereotype.Service;

@Service
public class CarbonEmissionService {

    private final EmissionFactorRepository emissionFactorRepository;

    public CarbonEmissionService(EmissionFactorRepository emissionFactorRepository) {
        this.emissionFactorRepository = emissionFactorRepository;
    }

    public double getEmissionFactor(TransportMode mode) {
        return emissionFactorRepository.findByTransportMode(mode)
                .map(EmissionFactor::getGramsCo2PerKm)
                .orElse(mode.getCo2GramsPerKm());
    }

    public double calculateBaselineEmissionGrams(double distanceKm) {
        double baselineFactor = getEmissionFactor(TransportMode.CAR_SOLO);
        return distanceKm * baselineFactor;
    }

    public double calculateModeEmissionGrams(TransportMode mode, double distanceKm) {
        double factor = getEmissionFactor(mode);
        return distanceKm * factor;
    }

    public double calculateCo2SavedGrams(TransportMode mode, double distanceKm) {
        double baseline = calculateBaselineEmissionGrams(distanceKm);
        double actual = calculateModeEmissionGrams(mode, distanceKm);
        return Math.max(0.0, baseline - actual);
    }

    public int calculateCaloriesBurned(TransportMode mode, double distanceKm) {
        return (int) Math.round(distanceKm * mode.getCaloriesPerKm());
    }

    public int calculatePoints(TransportMode mode, double co2SavedGrams, int currentStreakDays) {
        if (!mode.isSustainable() || co2SavedGrams <= 0) {
            return 0;
        }

        // Base points: 10 points per 1000g (1 kg) of CO2 saved
        double basePoints = (co2SavedGrams / 1000.0) * 10.0;

        // Multiply by transport mode bonus
        double pointsWithMode = basePoints * mode.getPointsMultiplier();

        // Streak bonus: +5% per streak day up to +50%
        double streakMultiplier = 1.0 + Math.min(0.50, currentStreakDays * 0.05);

        return (int) Math.max(1, Math.round(pointsWithMode * streakMultiplier));
    }
}
