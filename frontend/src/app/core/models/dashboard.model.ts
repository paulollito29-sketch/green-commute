export interface Badge {
  id: number;
  code: string;
  name: string;
  description: string;
  iconName: string;
  unlockedAt?: string;
}

export interface UserDashboard {
  userId: string;
  fullName: string;
  email: string;
  avatarUrl?: string;
  totalCo2SavedKg: number;
  totalDistanceKm: number;
  totalTrips: number;
  currentPoints: number;
  totalCalories: number;
  treesPlantedEquivalent: number;
  weeklyCo2SavedGrams: number[];
  weeklyLabels: string[];
  tripsByMode: Record<string, number>;
  unlockedBadges: Badge[];
  allBadges: Badge[];
}

export interface CommunityImpact {
  totalCo2SavedKg: number;
  totalDistanceKm: number;
  totalTrips: number;
  totalActiveUsers: number;
  treesPlantedEquivalent: number;
}

export interface LeaderboardEntry {
  userId: string;
  fullName: string;
  avatarUrl?: string;
  totalCo2SavedKg: number;
  currentPoints: number;
  rank: number;
}
