package com.ecocommute.dto.admin;

import com.ecocommute.entity.Role;
import java.time.LocalDateTime;

public record AdminUserDTO(
    String id,
    String email,
    String fullName,
    String avatarUrl,
    Role role,
    boolean active,
    String authProvider,
    int currentPoints,
    int currentLevel,
    int streakDays,
    double totalCo2SavedKg,
    int totalTrips,
    LocalDateTime createdAt,
    LocalDateTime lastTripDate
) {}
