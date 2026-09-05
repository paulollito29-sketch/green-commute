package com.ecocommute.controller;

import com.ecocommute.service.EcoRoutingService;
import com.ecocommute.service.RoutingEngineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
    public ResponseEntity<Map<String, Object>> planRoutes(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(routingEngineService.planRoutes(request, 1));
    }

    @PostMapping("/routes/eco-route")
    public ResponseEntity<Map<String, Object>> getInitialEcoRoute(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(ecoRoutingService.calculateInitialEcoRoute(request));
    }

    @PostMapping("/routes/recalculate")
    public ResponseEntity<Map<String, Object>> recalculateRoute(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(ecoRoutingService.recalculateRoute(request));
    }

    @PostMapping("/telemetry/tick")
    public ResponseEntity<Map<String, Object>> recordTelemetryTick(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(ecoRoutingService.processTelemetryTick(request));
    }
}
