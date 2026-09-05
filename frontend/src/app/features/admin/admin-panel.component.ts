import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminService } from '../../core/services/admin.service';
import { AdminKpis, AdminUserItem } from '../../core/models/admin.model';

@Component({
  selector: 'app-admin-panel',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="space-y-3 p-3 overflow-y-auto h-full">
      <div class="border-b border-purple-500/30 pb-2">
        <h2 class="text-xs font-bold text-purple-300">🛡️ Panel de Administración</h2>
      </div>

      <div class="grid grid-cols-3 gap-1.5 text-center">
        <div class="bg-slate-900/80 p-2 rounded-xl border border-purple-500/20">
          <div class="text-xs font-black text-purple-300">{{ kpis?.totalUsers || 0 }}</div>
          <div class="text-[8px] text-slate-400">Usuarios</div>
        </div>
        <div class="bg-slate-900/80 p-2 rounded-xl border border-purple-500/20">
          <div class="text-xs font-black text-purple-300">{{ kpis?.totalTrips || 0 }}</div>
          <div class="text-[8px] text-slate-400">Viajes</div>
        </div>
        <div class="bg-slate-900/80 p-2 rounded-xl border border-purple-500/20">
          <div class="text-xs font-black text-rose-400">{{ kpis?.suspiciousTripsCount || 0 }}</div>
          <div class="text-[8px] text-slate-400">Alertas</div>
        </div>
      </div>
    </div>
  `
})
export class AdminPanelComponent implements OnInit {
  kpis: AdminKpis | null = null;

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.adminService.getKpis().subscribe(k => this.kpis = k);
  }
}
