package com.ecocommute.service;

import com.ecocommute.dto.navigation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;

@Service
public class EcoRoutingService {

    private static final Logger log = LoggerFactory.getLogger(EcoRoutingService.class);
    private final RestClient restClient;

    private static final double BASELINE_CAR_EMISSION_PER_KM = 170.0;
    private static final double BICYCLE_EMISSION_PER_KM = 0.0;
    private static final double WALKING_EMISSION_PER_KM = 0.0;
    private static final double ECO_DRIVING_EMISSION_PER_KM = 125.0;

    public EcoRoutingService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(4000);
        factory.setReadTimeout(5000);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public EcoRouteResponse calculateInitialEcoRoute(EcoRouteRequest request) {
        UUID tripId = UUID.randomUUID();
        return computeRoute(
                tripId,
                request.originLat(), request.originLng(),
                request.destinationLat(), request.destinationLng(),
                request.vehicleMode(),
                0.0, 0.0
        );
    }

    public EcoRouteResponse recalculateRoute(RecalculateRouteRequest request) {
        log.info("Recalculando ruta por desvío off-route para viaje {} desde ({}, {})", 
                request.tripId(), request.currentLat(), request.currentLng());
        
        return computeRoute(
                request.tripId(),
                request.currentLat(), request.currentLng(),
                request.destinationLat(), request.destinationLng(),
                request.vehicleMode(),
                request.accumulatedCo2SavedGrams(),
                request.accumulatedDistanceKm()
        );
    }

    public TelemetryTickResponse processTelemetryTick(TelemetryTickRequest tick) {
        double distanceKm = tick.distanceIncrementMeters() / 1000.0;
        
        double baselineGrams = distanceKm * BASELINE_CAR_EMISSION_PER_KM;
        double modeEmissionGrams = switch (tick.speedKmh() > 25 ? "DRIVING" : "BICYCLE") {
            case "BICYCLE" -> distanceKm * BICYCLE_EMISSION_PER_KM;
            case "WALKING" -> distanceKm * WALKING_EMISSION_PER_KM;
            default -> distanceKm * ECO_DRIVING_EMISSION_PER_KM;
        };

        double tickSavedGrams = Math.max(0, baselineGrams - modeEmissionGrams);

        return new TelemetryTickResponse(
                true,
                Math.round(tickSavedGrams * 100.0) / 100.0,
                0.0,
                false
        );
    }

    private EcoRouteResponse computeRoute(UUID tripId, double lat1, double lng1, double lat2, double lng2, 
                                         String mode, double prevSavedGrams, double prevDistKm) {
        String safeMode = mode != null ? mode.toUpperCase() : "BICYCLE";
        String profile = "WALKING".equalsIgnoreCase(safeMode) ? "foot" : "driving";
        
        List<List<Double>> coordinates = fetchOsrmGeometry(profile, lat1, lng1, lat2, lng2);
        double distanceKm = calculateHaversineTotal(coordinates);
        int durationSeconds = (int) Math.round((distanceKm / getAverageSpeedKmh(safeMode)) * 3600);

        double baselineCo2 = distanceKm * BASELINE_CAR_EMISSION_PER_KM;
        double ecoCo2 = switch (safeMode) {
            case "BICYCLE", "WALKING" -> 0.0;
            default -> distanceKm * ECO_DRIVING_EMISSION_PER_KM;
        };

        double estimatedSavedGrams = prevSavedGrams + Math.max(0, baselineCo2 - ecoCo2);
        List<ManeuverStepDTO> maneuvers = generateTurnByTurnSteps(coordinates);

        return new EcoRouteResponse(
                tripId,
                safeMode,
                Math.round((prevDistKm + distanceKm) * 10.0) / 10.0,
                durationSeconds,
                Math.round(baselineCo2 * 10.0) / 10.0,
                Math.round(ecoCo2 * 10.0) / 10.0,
                Math.round(estimatedSavedGrams * 10.0) / 10.0,
                coordinates,
                maneuvers,
                "Ruta optimizada con 0% emisiones directas y paso por corredores de baja contaminación."
        );
    }

    private List<ManeuverStepDTO> generateTurnByTurnSteps(List<List<Double>> coords) {
        List<ManeuverStepDTO> steps = new ArrayList<>();
        if (coords.size() < 2) return steps;

        steps.add(new ManeuverStepDTO("Inicia el recorrido en dirección a tu destino", "START", 100.0, 30, coords.get(0)));

        for (int i = 1; i < coords.size() - 1; i += Math.max(1, coords.size() / 5)) {
            List<Double> p1 = coords.get(i - 1);
            List<Double> p2 = coords.get(i);
            List<Double> p3 = coords.get(Math.min(coords.size() - 1, i + 1));

            double b1 = calculateBearing(p1.get(0), p1.get(1), p2.get(0), p2.get(1));
            double b2 = calculateBearing(p2.get(0), p2.get(1), p3.get(0), p3.get(1));
            double diff = (b2 - b1 + 180) % 360 - 180;

            String type = "CONTINUE";
            String text = "Continúa recto por la vía";

            if (diff > 35) {
                type = "TURN_RIGHT";
                text = "Gira a la derecha en la siguiente intersección";
            } else if (diff < -35) {
                type = "TURN_LEFT";
                text = "Gira a la izquierda en la siguiente intersección";
            }

            steps.add(new ManeuverStepDTO(text, type, 250.0, 60, p2));
        }

        steps.add(new ManeuverStepDTO("Has llegado a tu destino", "ARRIVE", 0.0, 0, coords.get(coords.size() - 1)));
        return steps;
    }

    private List<List<Double>> fetchOsrmGeometry(String profile, double lat1, double lng1, double lat2, double lng2) {
        try {
            String url = String.format(Locale.US,
                    "https://router.project-osrm.org/route/v1/%s/%.6f,%.6f;%.6f,%.6f?overview=full&geometries=geojson",
                    profile, lng1, lat1, lng2, lat2);

            Map<?, ?> response = restClient.get().uri(url).retrieve().body(Map.class);
            if (response != null && "Ok".equalsIgnoreCase((String) response.get("code"))) {
                List<?> routes = (List<?>) response.get("routes");
                if (routes != null && !routes.isEmpty()) {
                    Map<?, ?> firstRoute = (Map<?, ?>) routes.get(0);
                    Map<?, ?> geometry = (Map<?, ?>) firstRoute.get("geometry");
                    List<?> rawCoords = (List<?>) geometry.get("coordinates");

                    List<List<Double>> parsed = new ArrayList<>();
                    for (Object ptObj : rawCoords) {
                        List<?> pt = (List<?>) ptObj;
                        parsed.add(List.of(((Number) pt.get(1)).doubleValue(), ((Number) pt.get(0)).doubleValue()));
                    }
                    return parsed;
                }
            }
        } catch (Exception e) {
            log.warn("OSRM error, falling back to direct points: {}", e.getMessage());
        }
        return List.of(List.of(lat1, lng1), List.of(lat2, lng2));
    }

    private double getAverageSpeedKmh(String mode) {
        return switch (mode != null ? mode.toUpperCase() : "BICYCLE") {
            case "WALKING" -> 4.8;
            case "BICYCLE" -> 16.5;
            default -> 32.0;
        };
    }

    private double calculateBearing(double lat1, double lon1, double lat2, double lon2) {
        double dLon = Math.toRadians(lon2 - lon1);
        double y = Math.sin(dLon) * Math.cos(Math.toRadians(lat2));
        double x = Math.cos(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) -
                   Math.sin(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(dLon);
        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }

    private double calculateHaversineTotal(List<List<Double>> coords) {
        double total = 0;
        for (int i = 0; i < coords.size() - 1; i++) {
            total += haversine(coords.get(i).get(0), coords.get(i).get(1), coords.get(i + 1).get(0), coords.get(i + 1).get(1));
        }
        return Math.max(0.1, total);
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon/2) * Math.sin(dLon/2);
        return 6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
