import { Injectable } from '@angular/core';
import { ApiService } from './api.service';
import { RoutePlanRequest, RoutePlanResponse, RouteOption } from '../models/route.model';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class RouteService {
  constructor(private api: ApiService) {}

  planRoute(request: RoutePlanRequest): Observable<RoutePlanResponse> {
    return this.api.post<RoutePlanResponse>('/routes/plan', request);
  }
}
