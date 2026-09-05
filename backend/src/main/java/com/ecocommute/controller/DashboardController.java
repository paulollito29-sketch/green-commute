package com.ecocommute.controller;

import com.ecocommute.entity.User;
import com.ecocommute.dto.dashboard.CommunityImpactDTO;
import com.ecocommute.dto.dashboard.UserDashboardDTO;
import com.ecocommute.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ResponseEntity<UserDashboardDTO> getUserDashboard(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(dashboardService.getUserDashboard(user.getId()));
    }

    @GetMapping({"/community-impact", "/community"})
    public ResponseEntity<CommunityImpactDTO> getCommunityImpact() {
        return ResponseEntity.ok(dashboardService.getCommunityImpact());
    }
}
