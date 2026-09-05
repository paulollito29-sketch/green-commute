package com.ecocommute.service;

import com.ecocommute.entity.TransportMode;
import com.ecocommute.dto.route.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

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
    @DisplayName("Debe calcular distancia Haversine correcta entre dos puntos geográficos")
    void testHaversineDistance() {
        // San Isidro (-12.0897, -77.0543) to Miraflores (-12.1215, -77.0298) ~ 4.4 km
        double distance = RoutingEngineService.calculateHaversineDistance(-12.0897, -77.0543, -12.1215, -77.0298);
        assertTrue(distance > 4.0 && distance < 5.0, "La distancia debe estar entre 4.0 y 5.0 km");
    }

    @Test
    @DisplayName("Debe generar respuesta en 3 fases: Baseline, Sostenible y Sugerencia IA")
    void testPlanRoutes3Phases() {
        CoordinatesDTO origin = new CoordinatesDTO(-12.0897, -77.0543, "Origen");
        CoordinatesDTO destination = new CoordinatesDTO(-12.0463, -77.0427, "Destino");
        RoutePlanRequest request = new RoutePlanRequest(origin, destination, List.of("BIKE", "TRANSIT"), true, 20, "BICYCLE", true);

        when(carbonEmissionService.calculateBaselineEmissionGrams(anyDouble())).thenReturn(1500.0);
        when(carbonEmissionService.calculateModeEmissionGrams(any(), anyDouble())).thenReturn(200.0);
        when(carbonEmissionService.calculateCo2SavedGrams(any(), anyDouble())).thenReturn(1300.0);
        when(carbonEmissionService.calculatePoints(any(), anyDouble(), anyInt())).thenReturn(35);
        when(carbonEmissionService.calculateCaloriesBurned(any(), anyDouble())).thenReturn(150);

        when(aiAdvisorService.generateRouteInsight(any(), any(), any(), anyDouble(), anyDouble(), anyInt(), anyBoolean()))
                .thenReturn(new AiInsightDTO("Ruta IA", "Explicación", "Insignia", 150.0, 0.05, "Clima óptimo"));

        RoutePlanResponse response = routingEngineService.planRoutes(request, 3);

        assertNotNull(response);
        assertNotNull(response.baselineRoute(), "Debe existir la ruta base en auto");
        assertEquals(TransportMode.CAR_SOLO, response.baselineRoute().mode());

        assertNotNull(response.sustainableRoute(), "Debe existir la opción sostenible");
        assertTrue(response.sustainableRoute().mode().isSustainable());

        assertNotNull(response.aiSmartRoute(), "Debe existir la sugerencia IA");
        assertTrue(response.aiSmartRoute().isAiRecommended());
        assertNotNull(response.aiSmartRoute().aiInsight());

        assertFalse(response.allOptions().isEmpty(), "Debe incluir todas las alternativas");
    }
}
