package com.ecocommute.dto.route;

public record CoordinatesDTO(
    double latitude,
    double longitude,
    String addressName
) {}
