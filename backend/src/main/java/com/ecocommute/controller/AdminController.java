package com.ecocommute.controller;

import com.ecocommute.entity.Role;
import com.ecocommute.dto.admin.*;
import com.ecocommute.service.AdminService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard/kpis")
    public ResponseEntity<AdminDashboardDTO> getAdminDashboardKPIs() {
        return ResponseEntity.ok(adminService.getAdminDashboardKPIs());
    }

    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserDTO>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(adminService.getUsers(page, size, search));
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<Map<String, String>> updateUserStatus(
            @PathVariable String id,
            @RequestBody Map<String, Boolean> body) {
        Boolean active = body.get("active");
        if (active != null) {
            adminService.updateUserStatus(id, active);
        }
        return ResponseEntity.ok(Map.of("message", "Estado de usuario actualizado"));
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<Map<String, String>> updateUserRole(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        String roleStr = body.get("role");
        if (roleStr != null) {
            adminService.updateUserRole(id, Role.valueOf(roleStr));
        }
        return ResponseEntity.ok(Map.of("message", "Rol de usuario actualizado"));
    }

    @GetMapping("/trips/audit")
    public ResponseEntity<Page<AdminTripAuditDTO>> getTripsForAudit(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean suspiciousOnly) {
        return ResponseEntity.ok(adminService.getTripsForAudit(page, size, suspiciousOnly));
    }

    @DeleteMapping("/trips/{id}")
    public ResponseEntity<Map<String, String>> deleteTripAndRevertStats(@PathVariable String id) {
        adminService.deleteTripAndRevertStats(id);
        return ResponseEntity.ok(Map.of("message", "Viaje anulado y estadísticas revertidas"));
    }

    @GetMapping("/settings/emission-factors")
    public ResponseEntity<List<EmissionFactorDTO>> getEmissionFactors() {
        return ResponseEntity.ok(adminService.getEmissionFactors());
    }

    @PutMapping("/settings/emission-factors/{id}")
    public ResponseEntity<EmissionFactorDTO> updateEmissionFactor(
            @PathVariable Long id,
            @RequestBody Map<String, Double> body) {
        Double grams = body.get("gramsCo2PerKm");
        if (grams == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(adminService.updateEmissionFactor(id, grams));
    }
}
