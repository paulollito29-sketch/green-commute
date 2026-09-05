package com.ecocommute.controller;

import com.ecocommute.entity.EmissionFactor;
import com.ecocommute.entity.Trip;
import com.ecocommute.entity.User;
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
    public ResponseEntity<Map<String, Object>> getAdminKpis() {
        return ResponseEntity.ok(adminService.getAdminKpis());
    }

    @GetMapping("/users")
    public ResponseEntity<Page<User>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(adminService.getUsers(page, size, search));
    }

    @PutMapping("/users/{userId}/toggle-status")
    public ResponseEntity<User> toggleUserStatus(@PathVariable String userId) {
        return ResponseEntity.ok(adminService.toggleUserStatus(userId));
    }

    @GetMapping("/trips/suspicious")
    public ResponseEntity<Page<Trip>> getSuspiciousTrips(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return ResponseEntity.ok(adminService.getSuspiciousTrips(page, size));
    }

    @GetMapping("/settings/emission-factors")
    public ResponseEntity<List<EmissionFactor>> getEmissionFactors() {
        return ResponseEntity.ok(adminService.getEmissionFactors());
    }

    @PutMapping("/settings/emission-factors/{id}")
    public ResponseEntity<EmissionFactor> updateEmissionFactor(
            @PathVariable Long id,
            @RequestParam double gramsCo2PerKm) {
        return ResponseEntity.ok(adminService.updateEmissionFactor(id, gramsCo2PerKm));
    }
}
