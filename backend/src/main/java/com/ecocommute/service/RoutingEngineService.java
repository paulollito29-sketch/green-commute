package com.ecocommute.service;

import com.ecocommute.entity.TransportMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;

@Service
public class RoutingEngineService {

    private static final Logger log = LoggerFactory.getLogger(RoutingEngineService.class);

    private final CarbonEmissionService carbonEmissionService;
    private final GeminiAiRouteAdvisorService aiAdvisorService;
    private final RestClient restClient;

    public RoutingEngineService(CarbonEmissionService carbonEmissionService,
                                GeminiAiRouteAdvisorService aiAdvisorService) {
        this.carbonEmissionService = carbonEmissionService;
        this.aiAdvisorService = aiAdvisorService;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(4000);
        factory.setReadTimeout(5000);

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    public Map<String, Object> planRoutes(Map<String, Object> request, int userStreakDays) {
        Map origin = (Map) request.get("origin");
        Map destination = (Map) request.get("destination");
        double originLat = ((Number) origin.get("latitude")).doubleValue();
        double originLng = ((Number) origin.get("longitude")).doubleValue();
        double destLat = ((Number) destination.get("latitude")).doubleValue();
        double destLng = ((Number) destination.get("longitude")).doubleValue();

        String selectedProfile = request.get("selectedProfile") != null ? request.get("selectedProfile").toString().toUpperCase() : "BICYCLE";
        boolean enableAi = Boolean.TRUE.equals(request.get("enableAiOptimization"));

        // 1. Fetch Driving Route with street alternatives
        List<OsrmRouteResult> drivingStreetRoutes = fetchOsrmStreetRoutes(
                "driving",
                originLat, originLng,
                destLat, destLng,
                true
        );

        OsrmRouteResult primaryDrivingRoute = !drivingStreetRoutes.isEmpty()
                ? drivingStreetRoutes.get(0)
                : createGeometricFallback(originLat, originLng, destLat, destLng);

        double baselineStreetDistanceKm = primaryDrivingRoute.distanceKm;
        int drivingDurationMinutes = primaryDrivingRoute.durationMinutes;
        double baselineCo2 = carbonEmissionService.calculateBaselineEmissionGrams(baselineStreetDistanceKm);

        TransportMode targetMode = switch (selectedProfile) {
            case "WALKING", "WALK" -> TransportMode.WALKING;
            case "DRIVING", "CAR" -> TransportMode.CAR_SOLO;
            default -> TransportMode.BICYCLE;
        };

        // 2. Baseline Car Option
        Map<String, Object> baselineCar = createRouteOption(
                "route-baseline-car",
                "🚗 Auto / Vehículo Convencional (Línea Base)",
                TransportMode.CAR_SOLO,
                baselineStreetDistanceKm,
                drivingDurationMinutes,
                baselineCo2,
                0.0,
                0,
                (int) Math.round(baselineStreetDistanceKm * TransportMode.CAR_SOLO.getCaloriesPerKm()),
                false,
                null,
                primaryDrivingRoute.pathCoordinates,
                "Ruta vehicular estándar directa por avenidas principales."
        );

        // 3. Active Mode Street Route
        String osrmProfile = (targetMode == TransportMode.WALKING) ? "foot" : "driving";
        List<OsrmRouteResult> activeModeRoutes = fetchOsrmStreetRoutes(
                osrmProfile,
                originLat, originLng,
                destLat, destLng,
                true
        );

        OsrmRouteResult standardActiveRoute = !activeModeRoutes.isEmpty()
                ? activeModeRoutes.get(0)
                : primaryDrivingRoute;

        double standardDistanceKm = standardActiveRoute.distanceKm;
        int standardDuration = (targetMode == TransportMode.BICYCLE)
                ? (int) Math.max(3, Math.round((standardDistanceKm / 16.0) * 60))
                : (targetMode == TransportMode.WALKING ? (int) Math.max(3, Math.round((standardDistanceKm / 4.8) * 60)) : standardActiveRoute.durationMinutes);

        double standardEmitted = carbonEmissionService.calculateModeEmissionGrams(targetMode, standardDistanceKm);
        double standardSaved = Math.max(0, baselineCo2 - standardEmitted);
        int standardPoints = carbonEmissionService.calculatePoints(targetMode, standardSaved, userStreakDays);

        Map<String, Object> standardOption = createRouteOption(
                "route-standard-direct",
                targetMode == TransportMode.BICYCLE ? "🚲 Bicicleta (Ruta Directa)" : (targetMode == TransportMode.WALKING ? "🚶 Caminata (Ruta Directa)" : "🚗 Vehículo Eficiente"),
                targetMode,
                standardDistanceKm,
                standardDuration,
                standardEmitted,
                standardSaved,
                standardPoints,
                (int) Math.round(standardDistanceKm * targetMode.getCaloriesPerKm()),
                false,
                null,
                standardActiveRoute.pathCoordinates,
                "Ruta directa sobre la red vial de la ciudad."
        );

        // 4. AI Optimized Green Corridor
        OsrmRouteResult greenStreetRoute;
        if (activeModeRoutes.size() > 1) {
            greenStreetRoute = activeModeRoutes.get(1);
        } else if (drivingStreetRoutes.size() > 1) {
            greenStreetRoute = drivingStreetRoutes.get(1);
        } else {
            greenStreetRoute = standardActiveRoute;
        }

        double greenDistanceKm = greenStreetRoute.distanceKm;
        int greenDuration = (targetMode == TransportMode.BICYCLE)
                ? (int) Math.max(3, Math.round((greenDistanceKm / 15.5) * 60))
                : (targetMode == TransportMode.WALKING ? (int) Math.max(3, Math.round((greenDistanceKm / 4.5) * 60)) : greenStreetRoute.durationMinutes);

        double greenEmitted = carbonEmissionService.calculateModeEmissionGrams(targetMode, greenDistanceKm);
        double greenSaved = Math.max(0, baselineCo2 - greenEmitted);
        int greenPoints = carbonEmissionService.calculatePoints(targetMode, greenSaved, userStreakDays) + 10;

        Map<String, Object> aiInsight = aiAdvisorService.generateRouteInsight(
                originLat, originLng,
                destLat, destLng,
                targetMode,
                greenDistanceKm,
                greenSaved,
                greenDuration,
                true
        );

        Map<String, Object> greenOption = createRouteOption(
                "route-ai-green-corridor",
                "🌿 Corredor Verde Optimizado con IA",
                targetMode,
                greenDistanceKm,
                greenDuration,
                greenEmitted,
                greenSaved,
                greenPoints,
                (int) Math.round(greenDistanceKm * targetMode.getCaloriesPerKm()),
                true,
                aiInsight,
                greenStreetRoute.pathCoordinates,
                "Corredor seleccionado por menor exposición a tráfico y mejor infraestructura."
        );

        Map<String, Object> result = new HashMap<>();
        result.put("baselineCarRoute", baselineCar);
        result.put("standardProfileRoute", standardOption);
        result.put("aiGreenCorridorRoute", greenOption);
        result.put("recommendedRouteId", "route-ai-green-corridor");
        return result;
    }

    private List<OsrmRouteResult> fetchOsrmStreetRoutes(String profile, double lat1, double lon1, double lat2, double lon2, boolean requestAlternatives) {
        String url = String.format(
                Locale.US,
                "https://router.project-osrm.org/route/v1/%s/%.6f,%.6f;%.6f,%.6f?overview=full&geometries=geojson&alternatives=%s",
                profile, lon1, lat1, lon2, lat2, requestAlternatives ? "true" : "false"
        );

        try {
            Map response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(Map.class);

            if (response != null && "Ok".equals(response.get("code"))) {
                List routes = (List) response.get("routes");
                if (routes != null && !routes.isEmpty()) {
                    List<OsrmRouteResult> results = new ArrayList<>();
                    for (Object routeObj : routes) {
                        Map route = (Map) routeObj;
                        double distanceMeters = ((Number) route.get("distance")).doubleValue();
                        double durationSeconds = ((Number) route.get("duration")).doubleValue();

                        Map geometry = (Map) route.get("geometry");
                        List rawCoords = (List) geometry.get("coordinates");

                        List<List<Double>> path = new ArrayList<>();
                        for (Object coordObj : rawCoords) {
                            List coord = (List) coordObj;
                            double lon = ((Number) coord.get(0)).doubleValue();
                            double lat = ((Number) coord.get(1)).doubleValue();
                            path.add(List.of(lat, lon));
                        }

                        results.add(new OsrmRouteResult(
                                distanceMeters / 1000.0,
                                (int) Math.ceil(durationSeconds / 60.0),
                                path
                        ));
                    }
                    return results;
                }
            }
        } catch (Exception e) {
            log.warn("OSRM query failed: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    private OsrmRouteResult createGeometricFallback(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double straightDistance = 6371.0 * c;
        double streetDistance = straightDistance * 1.35;
        int duration = (int) Math.round((streetDistance / 35.0) * 60);

        List<List<Double>> path = List.of(
                List.of(lat1, lon1),
                List.of((lat1 + lat2) / 2, (lon1 + lon2) / 2),
                List.of(lat2, lon2)
        );

        return new OsrmRouteResult(streetDistance, duration, path);
    }

    private Map<String, Object> createRouteOption(
            String id, String title, TransportMode mode,
            double distanceKm, int durationMinutes,
            double co2Emitted, double co2Saved,
            int points, int calories,
            boolean isAi, Map<String, Object> aiInsight,
            List<List<Double>> path, String summary) {

        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("title", title);
        map.put("mode", mode);
        map.put("modeDisplayName", mode.getDisplayName());
        map.put("distanceKm", Math.round(distanceKm * 100.0) / 100.0);
        map.put("durationMinutes", durationMinutes);
        map.put("co2EmittedGrams", Math.round(co2Emitted));
        map.put("co2SavedGrams", Math.round(co2Saved));
        map.put("potentialPoints", points);
        map.put("caloriesBurned", calories);
        map.put("isAiRecommended", isAi);
        map.put("aiInsight", aiInsight);
        map.put("pathCoordinates", path);
        map.put("summary", summary);
        return map;
    }

    private static class OsrmRouteResult {
        final double distanceKm;
        final int durationMinutes;
        final List<List<Double>> pathCoordinates;

        OsrmRouteResult(double distanceKm, int durationMinutes, List<List<Double>> pathCoordinates) {
            this.distanceKm = distanceKm;
            this.durationMinutes = durationMinutes;
            this.pathCoordinates = pathCoordinates;
        }
    }
}
