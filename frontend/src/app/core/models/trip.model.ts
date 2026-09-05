import { TransportMode } from './route.model';

export interface Trip {
  id: string;
  userId: string;
  userFullName: string;
  transportMode: TransportMode;
  transportModeDisplayName: string;
  originName: string;
  originLat: number;
  originLng: number;
  destinationName: string;
  destinationLat: number;
  destinationLng: number;
  distanceKm: number;
  durationMinutes: number;
  baselineCo2Grams: number;
  co2EmittedGrams: number;
  co2SavedGrams: number;
  caloriesBurned: number;
  pointsEarned: number;
  suspicious: boolean;
  suspiciousReason?: string;
  completedAt: string;
}

export interface TripCreateRequest {
  transportMode: TransportMode;
  originName?: string;
  originLat: number;
  originLng: number;
  destinationName?: string;
  destinationLat: number;
  destinationLng: number;
  distanceKm: number;
  durationMinutes: number;
}
