import { Injectable } from '@angular/core';
import { ApiService } from './api.service';
import { UserDashboard, CommunityImpact, LeaderboardEntry } from '../models/dashboard.model';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  constructor(private api: ApiService) {}

  getUserDashboard(): Observable<UserDashboard> {
    return this.api.get<UserDashboard>('/dashboard/summary');
  }

  getCommunityImpact(): Observable<CommunityImpact> {
    return this.api.get<CommunityImpact>('/dashboard/community');
  }

  getLeaderboard(): Observable<LeaderboardEntry[]> {
    return this.api.get<LeaderboardEntry[]>('/leaderboard');
  }
}
