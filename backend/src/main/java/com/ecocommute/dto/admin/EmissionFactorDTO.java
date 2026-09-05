package com.ecocommute.dto.admin;

import com.ecocommute.entity.TransportMode;

public record EmissionFactorDTO(
    Long id,
    TransportMode transportMode,
    String modeName,
    double gramsCo2PerKm,
    String description
) {}
