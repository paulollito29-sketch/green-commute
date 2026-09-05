package com.ecocommute.service;

import com.ecocommute.entity.TransportMode;
import com.ecocommute.dto.route.*;
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

    public RoutePlanResponse planRoutes(RoutePlanRequest request, int userStreakDays) {
        CoordinatesDTO origin = request.origin();
        CoordinatesDTO destination = request.destination();
        String selectedProfile = request.selectedProfile() != null ? request.selectedProfile().toUpperCase() : "BICYCLE";
        boolean enableAi = Boolean.TRUE.equals(request.enableAiOptimization());

        // 1. Fetch Driving Route with street alternatives (Respects one-way streets and real roads)
        List<OsrmRouteResult> drivingStreetRoutes = fetchOsrmStreetRoutes(
                "driving",
                origin.latitude(), origin.longitude(),
                destination.latitude(), destination.longitude(),
                true
        );

        OsrmRouteResult primaryDrivingRoute = !drivingStreetRoutes.isEmpty()
                ? drivingStreetRoutes.get(0)
                : createGeometricFallback(origin.latitude(), origin.longitude(), destination.latitude(), destination.longitude());

        double baselineStreetDistanceKm = primaryDrivingRoute.distanceKm;
        int drivingDurationMinutes = primaryDrivingRoute.durationMinutes;
        double baselineCo2 = carbonEmissionService.calculateBaselineEmissionGrams(baselineStreetDistanceKm);

        TransportMode targetMode = switch (selectedProfile) {
            case "WALKING", "WALK" -> TransportMode.WALKING;
            case "DRIVING", "CAR" -> TransportMode.CAR_SOLO;
            default -> TransportMode.BICYCLE;
        };

        // 2. Fetch specialized profile routes (foot / walking or driving)
        List<OsrmRouteResult> modeStreetRoutes;
        if (targetMode == TransportMode.WALKING) {
            modeStreetRoutes = fetchOsrmStreetRoutes(
                    "foot",
                    origin.latitude(), origin.longitude(),
                    destination.latitude(), destination.longitude(),
                    true
            );
            if (modeStreetRoutes.isEmpty()) modeStreetRoutes = drivingStreetRoutes;
        } else if (targetMode == TransportMode.BICYCLE) {
            // For bicycle, try foot (pedestrian corridors/alleys) and driving (streets)
            List<OsrmRouteResult> footRoutes = fetchOsrmStreetRoutes(
                    "foot",
                    origin.latitude(), origin.longitude(),
                    destination.latitude(), destination.longitude(),
                    true
            );
            modeStreetRoutes = !footRoutes.isEmpty() ? footRoutes : drivingStreetRoutes;
        } else {
            modeStreetRoutes = drivingStreetRoutes;
        }

        OsrmRouteResult standardRouteResult = !modeStreetRoutes.isEmpty()
                ? modeStreetRoutes.get(0)
                : primaryDrivingRoute;

        // 3. For the AI Green Corridor Route: Pick genuine second real street alternative from OSRM if available,
        // or the pedestrian/cycleway path. Never use artificial mathematical distortions.
        OsrmRouteResult aiGreenRouteResult;
        if (modeStreetRoutes.size() > 1) {
            aiGreenRouteResult = modeStreetRoutes.get(1);
        } else if (drivingStreetRoutes.size() > 1 && targetMode != TransportMode.WALKING) {
            aiGreenRouteResult = drivingStreetRoutes.get(1);
        } else {
            aiGreenRouteResult = standardRouteResult;
        }

        // 1. Baseline Route (Driving Solo)
        RouteOptionDTO baselineRoute = buildRouteOptionWithCoords(
                TransportMode.CAR_SOLO,
                baselineStreetDistanceKm,
                drivingDurationMinutes,
                false, null, userStreakDays,
                primaryDrivingRoute.coordinates,
                "Ruta Convencional (Línea Base)"
        );

        // 2. Standard Route for Selected Mode
        double stdDistance = standardRouteResult.distanceKm;
        int stdDuration = calculateDurationForMode(targetMode, stdDistance, standardRouteResult.durationMinutes);
        RouteOptionDTO standardSelectedRoute = buildRouteOptionWithCoords(
                targetMode,
                stdDistance,
                stdDuration,
                false, null, userStreakDays,
                standardRouteResult.coordinates,
                "Ruta Estándar (" + targetMode.getDisplayName() + ")"
        );

        // 3. AI Optimized Route (Real Street Green Corridor)
        double aiDistance = aiGreenRouteResult.distanceKm;
        int aiDuration = calculateDurationForMode(targetMode, aiDistance, aiGreenRouteResult.durationMinutes);

        AiInsightDTO aiInsight = null;
        if (enableAi) {
            double aiCo2Saved = carbonEmissionService.calculateCo2SavedGrams(targetMode, aiDistance);
            aiInsight = aiAdvisorService.generateRouteInsight(
                    origin, destination, targetMode, aiDistance, aiCo2Saved, aiDuration,
                    Boolean.TRUE.equals(request.hasBicycle())
            );
        } else {
            aiInsight = new AiInsightDTO(
                    "Corredor Verde Optimizado con IA",
                    "Ruta adaptada por calles arboladas, ciclovías y vías con menor exposición a material particulado.",
                    "🌿 Corredor Verde",
                    carbonEmissionService.calculateCaloriesBurned(targetMode, aiDistance),
                    Math.round((carbonEmissionService.calculateCo2SavedGrams(targetMode, aiDistance) / 21000.0) * 1000.0) / 1000.0,
                    "Flujo continuo y menor tráfico vehicular"
            );
        }

        RouteOptionDTO aiSmartRoute = buildRouteOptionWithCoords(
                targetMode,
                aiDistance,
                aiDuration,
                true,
                aiInsight,
                userStreakDays,
                aiGreenRouteResult.coordinates,
                "Ruta Verde IA (" + targetMode.getDisplayName() + ")"
        );

        List<RouteOptionDTO> allOptions = List.of(aiSmartRoute, standardSelectedRoute, baselineRoute);

        return new RoutePlanResponse(
                origin,
                destination,
                Math.round(baselineCo2 * 10.0) / 10.0,
                baselineRoute,
                standardSelectedRoute,
                aiSmartRoute,
                allOptions
        );
    }

    private int calculateDurationForMode(TransportMode mode, double distanceKm, int rawMinutes) {
        return switch (mode) {
            case WALKING -> (int) Math.max(3, Math.round((distanceKm / 4.5) * 60.0));
            case BICYCLE -> (int) Math.max(3, Math.round((distanceKm / 16.0) * 60.0));
            case CAR_SOLO -> Math.max(2, rawMinutes);
        };
    }

    private RouteOptionDTO buildRouteOptionWithCoords(TransportMode mode,
                                                     double distanceKm,
                                                     int durationMinutes,
                                                     boolean isAiRecommended,
                                                     AiInsightDTO aiInsight,
                                                     int userStreakDays,
                                                     List<List<Double>> realStreetPolyline,
                                                     String customTitle) {

        double emitted = carbonEmissionService.calculateModeEmissionGrams(mode, distanceKm);
        double saved = carbonEmissionService.calculateCo2SavedGrams(mode, distanceKm);
        int points = carbonEmissionService.calculatePoints(mode, saved, userStreakDays);
        int calories = carbonEmissionService.calculateCaloriesBurned(mode, distanceKm);

        String summary = String.format("%.1f km • %d min • %s",
                distanceKm, durationMinutes, customTitle);

        return new RouteOptionDTO(
                UUID.randomUUID().toString(),
                customTitle,
                mode,
                mode.getDisplayName(),
                Math.round(distanceKm * 10.0) / 10.0,
                durationMinutes,
                Math.round(emitted * 10.0) / 10.0,
                Math.round(saved * 10.0) / 10.0,
                points,
                calories,
                isAiRecommended,
                aiInsight,
                realStreetPolyline,
                summary
        );
    }

    private record OsrmRouteResult(double distanceKm, int durationMinutes, List<List<Double>> coordinates) {}

    private List<OsrmRouteResult> fetchOsrmStreetRoutes(String profile, double lat1, double lng1, double lat2, double lng2, boolean requestAlternatives) {
        List<OsrmRouteResult> results = new ArrayList<>();
        try {
            String osrmUrl = String.format(Locale.US,
                    "https://router.project-osrm.org/route/v1/%s/%.6f,%.6f;%.6f,%.6f?overview=full&geometries=geojson&alternatives=%s",
                    profile, lng1, lat1, lng2, lat2, requestAlternatives ? "true" : "false");

            Map<?, ?> response = restClient.get()
                    .uri(osrmUrl)
                    .retrieve()
                    .body(Map.class);

            if (response != null && "Ok".equalsIgnoreCase((String) response.get("code"))) {
                List<?> routes = (List<?>) response.get("routes");
                if (routes != null) {
                    for (Object rObj : routes) {
                        Map<?, ?> rMap = (Map<?, ?>) rObj;
                        double distanceMeters = ((Number) rMap.get("distance")).doubleValue();
                        double durationSeconds = ((Number) rMap.get("duration")).doubleValue();

                        Map<?, ?> geometry = (Map<?, ?>) rMap.get("geometry");
                        List<?> rawCoords = (List<?>) geometry.get("coordinates");

                        List<List<Double>> parsedCoords = new ArrayList<>();
                        for (Object coordObj : rawCoords) {
                            List<?> pt = (List<?>) coordObj;
                            double lon = ((Number) pt.get(0)).doubleValue();
                            double lat = ((Number) pt.get(1)).doubleValue();
                            parsedCoords.add(List.of(lat, lon));
                        }

                        double distanceKm = Math.max(0.1, distanceMeters / 1000.0);
                        int durationMinutes = (int) Math.max(1, Math.round(durationSeconds / 60.0));

                        results.add(new OsrmRouteResult(distanceKm, durationMinutes, parsedCoords));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("OSRM [{}] query failed: {}", profile, e.getMessage());
        }

        return results;
    }

    private OsrmRouteResult createGeometricFallback(double lat1, double lng1, double lat2, double lng2) {
        double direct = calculateHaversineDistance(lat1, lng1, lat2, lng2);
        double fallbackDist = Math.max(0.4, direct * 1.25);
        int fallbackDur = (int) Math.max(2, Math.round((fallbackDist / 20.0) * 60.0));

        List<List<Double>> points = new ArrayList<>();
        points.add(List.of(lat1, lng1));
        points.add(List.of((lat1 + lat2) / 2.0, (lng1 + lng2) / 2.0));
        points.add(List.of(lat2, lng2));

        return new OsrmRouteResult(fallbackDist, fallbackDur, points);
    }

    public static double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
