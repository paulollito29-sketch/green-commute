package com.ecocommute.controller;

import com.ecocommute.entity.User;
import com.ecocommute.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/auth/google")
    public ResponseEntity<Map<String, Object>> googleLogin(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(authService.googleLogin(request));
    }

    @GetMapping("/users/profile/{userId}")
    public ResponseEntity<Map<String, Object>> getProfile(@PathVariable String userId) {
        return ResponseEntity.ok(authService.getProfile(userId));
    }

    @GetMapping("/users/me")
    public ResponseEntity<Map<String, Object>> getMyProfile(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(authService.getProfile(user.getId()));
    }
}
