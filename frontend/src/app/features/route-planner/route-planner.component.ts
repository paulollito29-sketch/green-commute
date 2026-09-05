import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouteOption, TransportMode } from '../../core/models/route.model';

@Component({
  selector: 'app-route-planner',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="space-y-2.5 flex flex-col h-full">
      <!-- Mode Pills -->
      <div class="bg-slate-900/90 p-1 rounded-2xl border border-white/5 flex space-x-1 shadow-inner shrink-0">
        <button (click)="selectMode('BICYCLE')" [class.active]="selectedMode === 'BICYCLE'" class="flex-1 py-1.5 rounded-xl text-xs font-bold transition flex items-center justify-center space-x-1" [ngClass]="selectedMode === 'BICYCLE' ? 'text-white bg-slate-800' : 'text-slate-400 hover:text-white'">
          <span>🚲</span><span>Bicicleta</span>
        </button>
        <button (click)="selectMode('WALKING')" [class.active]="selectedMode === 'WALKING'" class="flex-1 py-1.5 rounded-xl text-xs font-semibold transition flex items-center justify-center space-x-1" [ngClass]="selectedMode === 'WALKING' ? 'text-white bg-slate-800' : 'text-slate-400 hover:text-white'">
          <span>🚶</span><span>Caminata</span>
        </button>
        <button (click)="selectMode('DRIVING')" [class.active]="selectedMode === 'DRIVING'" class="flex-1 py-1.5 rounded-xl text-xs font-semibold transition flex items-center justify-center space-x-1" [ngClass]="selectedMode === 'DRIVING' ? 'text-white bg-slate-800' : 'text-slate-400 hover:text-white'">
          <span>🚗</span><span>Vehículo</span>
        </button>
      </div>

      <!-- Search Inputs -->
      <div class="space-y-1.5 bg-slate-900/70 p-2.5 rounded-2xl border border-white/5 shrink-0">
        <div class="flex items-center space-x-2 bg-slate-950/80 px-2.5 py-1.5 rounded-xl border border-emerald-500/30">
          <span class="text-xs text-emerald-400">🔵</span>
          <input type="text" [value]="originText" readonly class="bg-transparent text-xs text-slate-200 w-full focus:outline-none" />
        </div>
        <div class="flex items-center space-x-2 bg-slate-950/90 px-2.5 py-2 rounded-xl border border-white/10">
          <span class="text-xs text-rose-400">🏁</span>
          <input [(ngModel)]="destinationQuery" (input)="onSearchInput()" placeholder="Buscar destino, parque o tocar mapa..." class="bg-transparent text-xs text-white w-full focus:outline-none" />
        </div>
      </div>

      <!-- AI Optimizer Button -->
      <button (click)="optimizeAi.emit()" class="w-full bg-gradient-to-r from-emerald-600 to-cyan-600 text-white text-xs font-bold py-2 rounded-xl shadow-md border border-teal-400/20 flex items-center justify-center space-x-1.5">
        <span>✨</span><span>Optimizar con IA (Gemini Advisor)</span>
      </button>

      <!-- Route Cards Container -->
      <div class="flex-1 space-y-1.5 overflow-y-auto max-h-48 md:max-h-64">
        <div *ngFor="let r of routes" (click)="selectRoute.emit(r.id)" [class.border-emerald-500]="selectedRouteId === r.id" class="p-3 rounded-2xl bg-slate-900/80 border border-white/10 hover:border-emerald-500/50 cursor-pointer transition">
          <div class="flex items-center justify-between">
            <span class="text-xs font-bold text-white">{{ r.title }}</span>
            <span class="text-[10px] font-extrabold text-emerald-400">+{{ r.potentialPoints }} pts</span>
          </div>
          <div class="flex items-center space-x-3 text-[11px] text-slate-400 mt-1">
            <span>{{ r.distanceKm }} km</span>
            <span>•</span>
            <span>{{ r.durationMinutes }} min</span>
            <span>•</span>
            <span class="text-emerald-400 font-bold">-{{ (r.co2SavedGrams / 1000).toFixed(2) }} kg CO₂</span>
          </div>
        </div>
      </div>

      <!-- Start Trip Action -->
      <button (click)="startTrip.emit()" class="w-full bg-gradient-to-r from-emerald-600 to-teal-500 hover:from-emerald-500 hover:to-teal-400 text-white font-bold py-3 px-4 rounded-2xl shadow-xl flex items-center justify-center space-x-2 text-xs transition cursor-pointer">
        <span class="text-base">🧭</span>
        <span>Iniciar Recorrido en Tiempo Real</span>
      </button>
    </div>
  `
})
export class RoutePlannerComponent {
  @Input() originText = 'Mi Ubicación (GPS)';
  @Input() destinationQuery = '';
  @Input() selectedMode: TransportMode = 'BICYCLE';
  @Input() routes: RouteOption[] = [];
  @Input() selectedRouteId: string | null = null;

  @Output() modeChanged = new EventEmitter<TransportMode>();
  @Output() searchChanged = new EventEmitter<string>();
  @Output() optimizeAi = new EventEmitter<void>();
  @Output() selectRoute = new EventEmitter<string>();
  @Output() startTrip = new EventEmitter<void>();

  selectMode(mode: TransportMode): void {
    this.selectedMode = mode;
    this.modeChanged.emit(mode);
  }

  onSearchInput(): void {
    this.searchChanged.emit(this.destinationQuery);
  }
}
