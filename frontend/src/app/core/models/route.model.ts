export type TransportMode = 'BICYCLE' | 'WALKING' | 'DRIVING' | 'CAR_SOLO';

export interface AiInsight {
  ecoReasoning: string;
  greenScore: number;
  safetyRating: number;
  shadeTreeCoveragePercent: number;
  cyclingInfrastructureQuality: string;
  healthBenefitSummary: string;
}

export interface RouteOption {
  id: string;
  title: string;
  mode: TransportMode;
  modeDisplayName: string;
  distanceKm: number;
  durationMinutes: number;
  co2EmittedGrams: number;
  co2SavedGrams: number;
  potentialPoints: number;
  caloriesBurned: number;
  isAiRecommended: boolean;
  aiInsight?: AiInsight;
  pathCoordinates: [number, number][]; // [lat, lng]
  summary: string;
}

export interface RoutePlanRequest {
  origin: {
    latitude: number;
    longitude: number;
    name?: string;
  };
  destination: {
    latitude: number;
    longitude: number;
    name?: string;
  };
  selectedProfile: TransportMode;
}

export interface RoutePlanResponse {
  baselineCarRoute: RouteOption;
  standardProfileRoute: RouteOption;
  aiGreenCorridorRoute: RouteOption;
  recommendedRouteId: string;
}
