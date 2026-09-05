import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardService } from '../../core/services/dashboard.service';
import { LeaderboardEntry } from '../../core/models/dashboard.model';

@Component({
  selector: 'app-leaderboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="space-y-3 p-3 overflow-y-auto h-full">
      <div class="border-b border-white/5 pb-2">
        <h2 class="text-xs font-bold text-white flex items-center gap-1">
          <span>🏆</span> Ranking & Comunidad
        </h2>
        <p class="text-[10px] text-slate-400">Líderes en movilidad verde</p>
      </div>

      <div class="space-y-1.5">
        <div *ngFor="let item of entries; let i = index" class="p-2.5 rounded-2xl bg-slate-900/70 border border-white/5 flex items-center justify-between">
          <div class="flex items-center space-x-2">
            <span class="w-5 h-5 rounded-full bg-slate-800 text-white text-[10px] font-bold flex items-center justify-center">
              {{ i + 1 }}
            </span>
            <span class="text-xs font-bold text-white">{{ item.fullName }}</span>
          </div>
          <span class="text-xs font-bold text-emerald-400">{{ item.totalCo2SavedKg }} kg CO₂</span>
        </div>
      </div>
    </div>
  `
})
export class LeaderboardComponent implements OnInit {
  entries: LeaderboardEntry[] = [];

  constructor(private dashboardService: DashboardService) {}

  ngOnInit(): void {
    this.dashboardService.getLeaderboard().subscribe(list => this.entries = list);
  }
}
