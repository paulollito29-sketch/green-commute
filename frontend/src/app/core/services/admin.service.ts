import { Injectable } from '@angular/core';
import { ApiService } from './api.service';
import { AdminKpis, AdminUserItem, EmissionFactorItem } from '../models/admin.model';
import { Trip } from '../models/trip.model';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  constructor(private api: ApiService) {}

  getKpis(): Observable<AdminKpis> {
    return this.api.get<AdminKpis>('/admin/dashboard/kpis');
  }

  getUsers(): Observable<{ content: AdminUserItem[] }> {
    return this.api.get<{ content: AdminUserItem[] }>('/admin/users');
  }

  getSuspiciousTrips(): Observable<{ content: Trip[] }> {
    return this.api.get<{ content: Trip[] }>('/admin/trips/suspicious');
  }

  getEmissionFactors(): Observable<EmissionFactorItem[]> {
    return this.api.get<EmissionFactorItem[]>('/admin/settings/emission-factors');
  }

  updateEmissionFactor(id: number, factor: number): Observable<EmissionFactorItem> {
    return this.api.put<EmissionFactorItem>(`/admin/settings/emission-factors/${id}?gramsCo2PerKm=${factor}`, {});
  }
}
