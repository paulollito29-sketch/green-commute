import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MapViewComponent } from './features/map/map-view.component';
import { NavHudComponent } from './features/navigation/nav-hud.component';
import { RoutePlannerComponent } from './features/route-planner/route-planner.component';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { LeaderboardComponent } from './features/leaderboard/leaderboard.component';
import { AdminPanelComponent } from './features/admin/admin-panel.component';
import { AuthModalComponent } from './features/auth/auth-modal.component';
import { AuthService } from './core/services/auth.service';
import { RouteService } from './core/services/route.service';
import { RouteOption, TransportMode } from './core/models/route.model';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    MapViewComponent,
    NavHudComponent,
    RoutePlannerComponent,
    DashboardComponent,
    LeaderboardComponent,
    AdminPanelComponent,
    AuthModalComponent
  ],
  templateUrl: './app.component.html'
})
export class AppComponent implements OnInit {
  activeTab = signal<'routes' | 'dashboard' | 'leaderboard' | 'admin'>('routes');
  showAuthModal = signal(false);
  isNavigating = signal(false);
  
  routes = signal<RouteOption[]>([]);
  selectedRouteId = signal<string | null>(null);
  selectedMode: TransportMode = 'BICYCLE';

  user = this.authService.currentUser;
  isAdmin = this.authService.isAdmin;

  constructor(
    private authService: AuthService,
    private routeService: RouteService
  ) {}

  ngOnInit(): void {
    this.calculateSampleRoute();
  }

  calculateSampleRoute(): void {
    this.routeService.planRoute({
      origin: { latitude: -12.0897, longitude: -77.0543, name: 'Mi Ubicación' },
      destination: { latitude: -12.0965, longitude: -77.0285, name: 'San Isidro' },
      selectedProfile: this.selectedMode
    }).subscribe(res => {
      const list = [res.aiGreenCorridorRoute, res.standardProfileRoute, res.baselineCarRoute];
      this.routes.set(list);
      this.selectedRouteId.set(res.aiGreenCorridorRoute.id);
    });
  }

  onModeChanged(mode: TransportMode): void {
    this.selectedMode = mode;
    this.calculateSampleRoute();
  }

  onDestinationPinned(dest: { lat: number; lng: number; name: string }): void {
    this.routeService.planRoute({
      origin: { latitude: -12.0897, longitude: -77.0543 },
      destination: { latitude: dest.lat, longitude: dest.lng },
      selectedProfile: this.selectedMode
    }).subscribe(res => {
      this.routes.set([res.aiGreenCorridorRoute, res.standardProfileRoute, res.baselineCarRoute]);
      this.selectedRouteId.set(res.aiGreenCorridorRoute.id);
    });
  }

  startNavigation(): void {
    this.isNavigating.set(true);
  }

  stopNavigation(): void {
    this.isNavigating.set(false);
  }
}
