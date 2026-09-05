package com.ecocommute.controller;

import com.ecocommute.entity.Trip;
import com.ecocommute.entity.User;
import com.ecocommute.dto.trip.TripCreateRequest;
import com.ecocommute.dto.trip.TripDTO;
import com.ecocommute.repository.TripRepository;
import com.ecocommute.service.GamificationService;
import jakarta.validation.Valid;
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
    public ResponseEntity<TripDTO> recordTrip(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody TripCreateRequest request) {

        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(gamificationService.recordTrip(user.getId(), request));
    }

    @GetMapping("/history")
    public ResponseEntity<Page<TripDTO>> getMyTrips(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "completedAt"));
        Page<Trip> trips = tripRepository.findByUserIdOrderByCompletedAtDesc(user.getId(), pageRequest);

        return ResponseEntity.ok(trips.map(trip -> new TripDTO(
                trip.getId(),
                user.getId(),
                user.getFullName(),
                trip.getTransportMode(),
                trip.getTransportMode().getDisplayName(),
                trip.getOriginName(),
                trip.getOriginLat(),
                trip.getOriginLng(),
                trip.getDestinationName(),
                trip.getDestinationLat(),
                trip.getDestinationLng(),
                trip.getDistanceKm(),
                trip.getDurationMinutes(),
                trip.getBaselineCo2Grams(),
                trip.getCo2EmittedGrams(),
                trip.getCo2SavedGrams(),
                trip.getCaloriesBurned(),
                trip.getPointsEarned(),
                trip.isSuspicious(),
                trip.getSuspiciousReason(),
                trip.getCompletedAt()
        )));
    }
}
