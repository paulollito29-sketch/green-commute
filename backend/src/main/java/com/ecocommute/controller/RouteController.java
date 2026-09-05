package com.ecocommute.controller;

import com.ecocommute.dto.navigation.*;
import com.ecocommute.dto.route.RoutePlanRequest;
import com.ecocommute.dto.route.RoutePlanResponse;
import com.ecocommute.service.EcoRoutingService;
import com.ecocommute.service.RoutingEngineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1", "/api"})
@CrossOrigin(origins = "*")
public class RouteController {

    private final EcoRoutingService ecoRoutingService;
    private final RoutingEngineService routingEngineService;

    public RouteController(EcoRoutingService ecoRoutingService,
                           RoutingEngineService routingEngineService) {
        this.ecoRoutingService = ecoRoutingService;
        this.routingEngineService = routingEngineService;
    }

    @PostMapping("/routes/plan")
    public ResponseEntity<RoutePlanResponse> planRoutes(@RequestBody RoutePlanRequest request) {
        return ResponseEntity.ok(routingEngineService.planRoutes(request, 1));
    }

    @PostMapping("/routes/eco-route")
    public ResponseEntity<EcoRouteResponse> getInitialEcoRoute(@RequestBody EcoRouteRequest request) {
        return ResponseEntity.ok(ecoRoutingService.calculateInitialEcoRoute(request));
    }

    @PostMapping("/routes/recalculate")
    public ResponseEntity<EcoRouteResponse> recalculateRoute(@RequestBody RecalculateRouteRequest request) {
        return ResponseEntity.ok(ecoRoutingService.recalculateRoute(request));
    }

    @PostMapping("/telemetry/tick")
    public ResponseEntity<TelemetryTickResponse> recordTelemetryTick(@RequestBody TelemetryTickRequest request) {
        return ResponseEntity.ok(ecoRoutingService.processTelemetryTick(request));
    }
}
