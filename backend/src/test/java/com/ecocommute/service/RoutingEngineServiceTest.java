package com.ecocommute.service;

import com.ecocommute.entity.TransportMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoutingEngineServiceTest {

    @Mock
    private CarbonEmissionService carbonEmissionService;

    @Mock
    private GeminiAiRouteAdvisorService aiAdvisorService;

    private RoutingEngineService routingEngineService;

    @BeforeEach
    void setUp() {
        routingEngineService = new RoutingEngineService(carbonEmissionService, aiAdvisorService);
    }

    @Test
    @DisplayName("Debe generar plan de rutas con 3 alternativas (Baseline, Directa, Corredor Verde) sin DTO")
    void testPlanRoutesSuccess() {
        Map<String, Object> req = new HashMap<>();
        req.put("origin", Map.of("latitude", -12.0897, "longitude", -77.0543));
        req.put("destination", Map.of("latitude", -12.0965, "longitude", -77.0285));
        req.put("selectedProfile", "BICYCLE");

        when(carbonEmissionService.calculateBaselineEmissionGrams(anyDouble())).thenReturn(1445.0);
        when(carbonEmissionService.calculateModeEmissionGrams(eq(TransportMode.BICYCLE), anyDouble())).thenReturn(0.0);
        when(carbonEmissionService.calculatePoints(eq(TransportMode.BICYCLE), anyDouble(), anyInt())).thenReturn(45);
        when(aiAdvisorService.generateRouteInsight(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any(), anyDouble(), anyDouble(), anyInt(), anyBoolean()))
                .thenReturn(Map.of("ecoReasoning", "Ruta de prueba", "greenScore", 95));

        Map<String, Object> response = routingEngineService.planRoutes(req, 1);

        assertNotNull(response);
        assertNotNull(response.get("baselineCarRoute"));
        assertNotNull(response.get("standardProfileRoute"));
        assertNotNull(response.get("aiGreenCorridorRoute"));

        Map<String, Object> green = (Map<String, Object>) response.get("aiGreenCorridorRoute");
        assertTrue((Boolean) green.get("isAiRecommended"));
    }
}
