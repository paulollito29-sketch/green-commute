import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-nav-hud',
  standalone: true,
  imports: [CommonModule],
  template: `
    <!-- Top Turn Banner (Waze Style) -->
    <div class="absolute top-4 left-4 right-4 md:left-auto md:right-6 md:w-[420px] z-40 bg-slate-950/95 backdrop-blur-2xl border-2 border-brand-500/60 rounded-3xl p-4 shadow-2xl flex items-center justify-between space-x-3.5">
      <div class="flex items-center space-x-3.5 min-w-0 flex-1">
        <div class="w-12 h-12 rounded-2xl bg-gradient-to-br from-brand-600 to-emerald-500 text-white flex items-center justify-center text-2xl shadow-lg border border-white/20">
          {{ instructionIcon }}
        </div>
        <div class="min-w-0 flex-1">
          <div class="flex items-baseline space-x-1.5">
            <span class="text-2xl font-black text-white tracking-tight">{{ countdownMeters }}</span>
            <span class="text-xs font-black text-brand-400 uppercase">m</span>
          </div>
          <div class="text-xs font-extrabold text-white truncate leading-tight">
            {{ instructionText }}
          </div>
          <div class="text-[10px] text-slate-400 truncate mt-0.5">
            {{ nextStreet }}
          </div>
        </div>
      </div>

      <div class="flex flex-col items-end space-y-1.5 shrink-0">
        <button (click)="toggleVoice.emit()" class="w-9 h-9 rounded-2xl bg-slate-800 hover:bg-slate-700 text-white flex items-center justify-center text-sm border border-white/10 shadow-md">
          {{ isMuted ? '🔇' : '🔊' }}
        </button>
        <span class="text-[10px] font-black text-emerald-400 px-2 py-0.5 rounded-full bg-emerald-500/10 border border-emerald-500/30">
          {{ etaMinutes }} min
        </span>
      </div>
    </div>

    <!-- Speedometer -->
    <div class="absolute bottom-24 left-4 md:bottom-8 md:left-6 z-30 flex items-center space-x-2.5 bg-slate-950/90 backdrop-blur-xl border border-white/10 rounded-2xl p-2.5 shadow-2xl">
      <div class="w-11 h-11 rounded-xl bg-slate-900 border border-brand-500/40 flex flex-col items-center justify-center text-center shadow-inner">
        <span class="text-base font-black text-white leading-none">{{ speedKmh }}</span>
        <span class="text-[7px] font-bold text-slate-400 uppercase leading-none mt-0.5">km/h</span>
      </div>
      <div>
        <div class="text-[10px] font-bold text-slate-200">
          🌱 Movilidad Sostenible
        </div>
        <div class="text-[8px] font-extrabold text-emerald-400">
          0% Emisiones de Carbono
        </div>
      </div>
    </div>
  `
})
export class NavHudComponent {
  @Input() instructionIcon = '⬆️';
  @Input() instructionText = 'Sigue la ruta en el mapa';
  @Input() nextStreet = 'Avanza hacia tu destino';
  @Input() countdownMeters = 150;
  @Input() speedKmh = 18;
  @Input() etaMinutes = 12;
  @Input() isMuted = false;
  @Output() toggleVoice = new EventEmitter<void>();
}
