package com.ecocommute.service;

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

    public Map<String, Object> calculateInitialEcoRoute(Map<String, Object> request) {
        UUID tripId = UUID.randomUUID();
        double originLat = ((Number) request.get("originLat")).doubleValue();
        double originLng = ((Number) request.get("originLng")).doubleValue();
        double destLat = ((Number) request.get("destinationLat")).doubleValue();
        double destLng = ((Number) request.get("destinationLng")).doubleValue();
        String vehicleMode = (String) request.get("vehicleMode");

        return computeRoute(tripId, originLat, originLng, destLat, destLng, vehicleMode, 0.0, 0.0);
    }

    public Map<String, Object> recalculateRoute(Map<String, Object> request) {
        UUID tripId = request.get("tripId") != null ? UUID.fromString(request.get("tripId").toString()) : UUID.randomUUID();
        double currentLat = ((Number) request.get("currentLat")).doubleValue();
        double currentLng = ((Number) request.get("currentLng")).doubleValue();
        double destLat = ((Number) request.get("destinationLat")).doubleValue();
        double destLng = ((Number) request.get("destinationLng")).doubleValue();
        String vehicleMode = (String) request.get("vehicleMode");
        double accCo2 = request.get("accumulatedCo2SavedGrams") != null ? ((Number) request.get("accumulatedCo2SavedGrams")).doubleValue() : 0.0;
        double accDist = request.get("accumulatedDistanceKm") != null ? ((Number) request.get("accumulatedDistanceKm")).doubleValue() : 0.0;

        return computeRoute(tripId, currentLat, currentLng, destLat, destLng, vehicleMode, accCo2, accDist);
    }

    public Map<String, Object> processTelemetryTick(Map<String, Object> tick) {
        double distIncrement = ((Number) tick.get("distanceIncrementMeters")).doubleValue();
        double distanceKm = distIncrement / 1000.0;
        double speedKmh = ((Number) tick.get("speedKmh")).doubleValue();

        double baselineGrams = distanceKm * BASELINE_CAR_EMISSION_PER_KM;
        double modeEmissionGrams = switch (speedKmh > 25 ? "DRIVING" : "BICYCLE") {
            case "BICYCLE" -> distanceKm * BICYCLE_EMISSION_PER_KM;
            case "WALKING" -> distanceKm * WALKING_EMISSION_PER_KM;
            default -> distanceKm * ECO_DRIVING_EMISSION_PER_KM;
        };

        double deltaCo2Saved = Math.max(0.0, baselineGrams - modeEmissionGrams);
        double accCo2 = tick.get("accumulatedCo2SavedGrams") != null ? ((Number) tick.get("accumulatedCo2SavedGrams")).doubleValue() : 0.0;
        double totalCo2Saved = accCo2 + deltaCo2Saved;

        double treesEquivalent = (totalCo2Saved / 1000.0) / 21.77;
        double totalDist = (tick.get("accumulatedDistanceKm") != null ? ((Number) tick.get("accumulatedDistanceKm")).doubleValue() : 0.0) + distanceKm;
        int ecoPointsEarned = (int) Math.round(totalCo2Saved / 15.0);

        Map<String, Object> response = new HashMap<>();
        response.put("tripId", tick.get("tripId"));
        response.put("deltaCo2SavedGrams", deltaCo2Saved);
        response.put("totalCo2SavedGrams", totalCo2Saved);
        response.put("treesEquivalent", treesEquivalent);
        response.put("totalDistanceKm", totalDist);
        response.put("currentSpeedKmh", speedKmh);
        response.put("ecoPointsEarned", ecoPointsEarned);
        response.put("nextManeuverDistanceMeters", 120.0);
        response.put("voiceAnnouncementText", "Continúa por 120 metros.");
        response.put("suggestedManeuver", "CONTINUE_STRAIGHT");
        return response;
    }

    private Map<String, Object> computeRoute(UUID tripId, double lat1, double lon1, double lat2, double lon2,
                                             String mode, double accSavedCo2, double accDistKm) {
        String profile = switch (mode != null ? mode.toUpperCase() : "BICYCLE") {
            case "WALKING", "FOOT" -> "foot";
            case "DRIVING", "CAR" -> "driving";
            default -> "bike";
        };

        String url = String.format(Locale.US, "https://router.project-osrm.org/route/v1/%s/%.6f,%.6f;%.6f,%.6f?overview=full&geometries=geojson&steps=true",
                profile.equals("bike") ? "driving" : profile, lon1, lat1, lon2, lat2);

        List<List<Double>> pathCoordinates = new ArrayList<>();
        double remainingDistanceMeters = 1000.0;
        int remainingDurationSeconds = 300;

        try {
            Map response = restClient.get().uri(url).retrieve().body(Map.class);
            if (response != null && "Ok".equals(response.get("code"))) {
                List routes = (List) response.get("routes");
                if (routes != null && !routes.isEmpty()) {
                    Map primary = (Map) routes.get(0);
                    remainingDistanceMeters = ((Number) primary.get("distance")).doubleValue();
                    remainingDurationSeconds = ((Number) primary.get("duration")).intValue();

                    Map geometry = (Map) primary.get("geometry");
                    List rawCoords = (List) geometry.get("coordinates");
                    for (Object pt : rawCoords) {
                        List c = (List) pt;
                        pathCoordinates.add(List.of(((Number) c.get(1)).doubleValue(), ((Number) c.get(0)).doubleValue()));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("OSRM lookup error: {}", e.getMessage());
            pathCoordinates.add(List.of(lat1, lon1));
            pathCoordinates.add(List.of(lat2, lon2));
        }

        double remainingKm = remainingDistanceMeters / 1000.0;
        double baselineGrams = remainingKm * BASELINE_CAR_EMISSION_PER_KM;
        double modeGrams = remainingKm * (mode != null && mode.equalsIgnoreCase("WALKING") ? WALKING_EMISSION_PER_KM : BICYCLE_EMISSION_PER_KM);
        double remainingSaved = Math.max(0, baselineGrams - modeGrams);

        Map<String, Object> result = new HashMap<>();
        result.put("tripId", tripId);
        result.put("remainingDistanceMeters", remainingDistanceMeters);
        result.put("remainingDurationSeconds", remainingDurationSeconds);
        result.put("totalEstimatedCo2SavedGrams", accSavedCo2 + remainingSaved);
        result.put("polylineCoordinates", pathCoordinates);
        result.put("currentInstruction", "Iniciando recorrido ecológico...");
        result.put("turnDirection", "CONTINUE");
        result.put("nextStreetName", "Vía Principal");
        result.put("distanceToNextStepMeters", 150.0);
        return result;
    }
}
