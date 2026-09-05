import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardService } from '../../core/services/dashboard.service';
import { UserDashboard } from '../../core/models/dashboard.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="space-y-3 p-3 overflow-y-auto h-full">
      <div class="flex items-center justify-between border-b border-white/5 pb-2">
        <div>
          <h2 class="text-xs font-bold text-white flex items-center gap-1">
            <span>📊</span> Mi Impacto Ambiental
          </h2>
          <p class="text-[10px] text-slate-400">Métricas ODS 11 personales</p>
        </div>
        <span class="text-[9px] px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-300 border border-emerald-500/30 font-semibold">
          Nivel 1
        </span>
      </div>

      <div class="grid grid-cols-2 gap-2">
        <div class="bg-slate-900/70 p-2.5 rounded-2xl border border-white/5">
          <div class="text-[9px] text-emerald-400 font-semibold">CO₂ Ahorrado</div>
          <div class="text-lg font-black text-white mt-0.5">{{ data?.totalCo2SavedKg || 0 }} <span class="text-xs text-emerald-400">kg</span></div>
        </div>
        <div class="bg-slate-900/70 p-2.5 rounded-2xl border border-white/5">
          <div class="text-[9px] text-teal-400 font-semibold">Árboles Eq.</div>
          <div class="text-lg font-black text-white mt-0.5">{{ data?.treesPlantedEquivalent || 0 }} <span class="text-[9px] text-teal-400">árboles</span></div>
        </div>
        <div class="bg-slate-900/70 p-2.5 rounded-2xl border border-white/5">
          <div class="text-[9px] text-cyan-400 font-semibold">Distancia</div>
          <div class="text-lg font-black text-white mt-0.5">{{ data?.totalDistanceKm || 0 }} <span class="text-xs text-cyan-400">km</span></div>
        </div>
        <div class="bg-slate-900/70 p-2.5 rounded-2xl border border-white/5">
          <div class="text-[9px] text-amber-400 font-semibold">EcoPuntos</div>
          <div class="text-lg font-black text-white mt-0.5">{{ data?.currentPoints || 0 }} <span class="text-xs text-amber-400">pts</span></div>
        </div>
      </div>
    </div>
  `
})
export class DashboardComponent implements OnInit {
  data: UserDashboard | null = null;

  constructor(private dashboardService: DashboardService) {}

  ngOnInit(): void {
    this.dashboardService.getUserDashboard().subscribe(d => this.data = d);
  }
}
