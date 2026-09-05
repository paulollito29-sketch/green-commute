package com.ecocommute.controller;

import com.ecocommute.entity.Trip;
import com.ecocommute.entity.User;
import com.ecocommute.repository.TripRepository;
import com.ecocommute.service.GamificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trips")
public class TripController {

    private final GamificationService gamificationService;
    private final TripRepository tripRepository;

    public TripController(GamificationService gamificationService, TripRepository tripRepository) {
        this.gamificationService = gamificationService;
        this.tripRepository = tripRepository;
    }

    @PostMapping
    public ResponseEntity<Trip> recordTrip(
            @AuthenticationPrincipal User user,
            @RequestBody Trip trip) {

        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(gamificationService.recordTrip(user.getId(), trip));
    }

    @GetMapping("/history")
    public ResponseEntity<Page<Trip>> getMyTrips(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "completedAt"));
        Page<Trip> trips = tripRepository.findByUserIdOrderByCompletedAtDesc(user.getId(), pageRequest);
        return ResponseEntity.ok(trips);
    }
}
