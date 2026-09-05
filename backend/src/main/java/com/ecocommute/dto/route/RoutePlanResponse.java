package com.ecocommute.dto.route;

import java.util.List;

public record RoutePlanResponse(
    CoordinatesDTO origin,
    CoordinatesDTO destination,
    double baselineCo2Grams,
    RouteOptionDTO baselineRoute,
    RouteOptionDTO sustainableRoute,
    RouteOptionDTO aiSmartRoute,
    List<RouteOptionDTO> allOptions
) {}
