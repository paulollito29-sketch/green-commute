import { Role } from './user.model';
import { TransportMode } from './route.model';
import { Trip } from './trip.model';

export interface AdminUserItem {
  id: string;
  email: string;
  fullName: string;
  role: Role;
  active: boolean;
  totalPoints: number;
  totalTrips: number;
  totalCo2SavedKg: number;
  registeredAt: string;
}

export interface EmissionFactorItem {
  id: number;
  transportMode: TransportMode;
  name: string;
  gramsCo2PerKm: number;
  active: boolean;
}

export interface AdminKpis {
  totalUsers: number;
  totalTrips: number;
  totalCo2SavedKg: number;
  suspiciousTripsCount: number;
}
