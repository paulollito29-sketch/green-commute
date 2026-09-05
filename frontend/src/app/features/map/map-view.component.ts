import { Component, OnInit, OnDestroy, ElementRef, ViewChild, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import * as L from 'leaflet';
import { RouteOption } from '../../core/models/route.model';

@Component({
  selector: 'app-map-view',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="w-full h-full relative overflow-hidden bg-[#060913]">
      <div #mapContainer class="w-full h-full z-10"></div>
    </div>
  `
})
export class MapViewComponent implements OnInit, OnDestroy {
  @ViewChild('mapContainer', { static: true }) mapContainer!: ElementRef<HTMLDivElement>;
  @Input() set routes(routesList: RouteOption[]) {
    this.drawRoutes(routesList);
  }
  @Input() selectedRouteId: string | null = null;
  @Output() routeSelected = new EventEmitter<string>();
  @Output() destinationPinned = new EventEmitter<{ lat: number; lng: number; name: string }>();

  private map!: L.Map;
  private polylines: L.Polyline[] = [];
  private userMarker: L.Marker | null = null;
  private destMarker: L.Marker | null = null;

  ngOnInit(): void {
    this.initMap();
  }

  private initMap(): void {
    this.map = L.map(this.mapContainer.nativeElement, {
      center: [-12.0897, -77.0543],
      zoom: 13,
      zoomControl: false
    });

    L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
      maxZoom: 19,
      subdomains: 'abcd'
    }).addTo(this.map);

    this.map.on('click', (e: L.LeafletMouseEvent) => {
      this.setDestination(e.latlng.lat, e.latlng.lng, 'Destino en Mapa');
      this.destinationPinned.emit({ lat: e.latlng.lat, lng: e.latlng.lng, name: 'Destino en Mapa' });
    });
  }

  public setUserPosition(lat: number, lng: number): void {
    if (!this.map) return;
    if (!this.userMarker) {
      const pulseIcon = L.divIcon({
        className: 'gps-pulse-marker',
        iconSize: [18, 18],
        iconAnchor: [9, 9]
      });
      this.userMarker = L.marker([lat, lng], { icon: pulseIcon }).addTo(this.map);
    } else {
      this.userMarker.setLatLng([lat, lng]);
    }
  }

  public setDestination(lat: number, lng: number, name: string): void {
    if (this.destMarker) {
      this.map.removeLayer(this.destMarker);
    }
    const destIcon = L.divIcon({
      className: 'dest-pin',
      html: '<div style="font-size:24px; filter:drop-shadow(0 4px 8px rgba(0,0,0,0.6));">🏁</div>',
      iconSize: [30, 30],
      iconAnchor: [15, 26]
    });
    this.destMarker = L.marker([lat, lng], { icon: destIcon }).addTo(this.map);
  }

  public drawRoutes(routes: RouteOption[]): void {
    if (!this.map) return;
    this.polylines.forEach(p => this.map.removeLayer(p));
    this.polylines = [];

    if (!routes || routes.length === 0) return;

    routes.forEach(route => {
      const isSelected = this.selectedRouteId ? route.id === this.selectedRouteId : route.isAiRecommended;
      const color = isSelected ? (route.isAiRecommended ? '#10b981' : '#38bdf8') : '#475569';
      const weight = isSelected ? 6 : 4;

      const poly = L.polyline(route.pathCoordinates, {
        color,
        weight,
        opacity: isSelected ? 0.95 : 0.45,
        lineCap: 'round',
        lineJoin: 'round'
      }).addTo(this.map);

      poly.on('click', () => {
        this.routeSelected.emit(route.id);
      });

      this.polylines.push(poly);
    });

    const active = routes.find(r => r.id === this.selectedRouteId) || routes.find(r => r.isAiRecommended);
    if (active && active.pathCoordinates.length > 0) {
      this.map.fitBounds(L.latLngBounds(active.pathCoordinates), { padding: [60, 60], maxZoom: 16 });
    }
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
    }
  }
}
