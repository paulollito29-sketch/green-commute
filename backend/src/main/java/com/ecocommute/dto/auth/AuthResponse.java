package com.ecocommute.dto.auth;

import com.ecocommute.entity.Role;

public record AuthResponse(
    String token,
    String tokenType,
    String userId,
    String email,
    String fullName,
    String avatarUrl,
    Role role,
    int currentPoints,
    int currentLevel,
    int streakDays
) {}
