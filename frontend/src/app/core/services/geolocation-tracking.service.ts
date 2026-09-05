import { Injectable } from '@angular/core';
import { Observable, Subject, throttleTime, distinctUntilChanged } from 'rxjs';

export interface GpsPosition {
  lat: number;
  lng: number;
  accuracy: number;
  speed: number;
  heading: number;
  timestamp: number;
}

@Injectable({
  providedIn: 'root'
})
export class GeolocationTrackingService {
  private watchId: number | null = null;
  private positionSubject = new Subject<GpsPosition>();

  // RxJS Stream of user positions with High Accuracy and Throttling
  public position$ = this.positionSubject.asObservable().pipe(
    throttleTime(1000), // Max 1 emission per second to avoid backend overload
    distinctUntilChanged((prev, curr) => {
      // Emit if moved > 2 meters or speed changed
      const dist = this.calculateDistanceMeters(prev.lat, prev.lng, curr.lat, curr.lng);
      return dist < 2.0 && Math.abs(prev.speed - curr.speed) < 1.0;
    })
  );

  startTracking(): void {
    if (!('geolocation' in navigator)) {
      console.warn('Geolocation not supported in browser');
      return;
    }

    if (this.watchId !== null) return;

    this.watchId = navigator.geolocation.watchPosition(
      pos => {
        const speedKmh = pos.coords.speed !== null ? Math.max(0, pos.coords.speed * 3.6) : 0;
        this.positionSubject.next({
          lat: pos.coords.latitude,
          lng: pos.coords.longitude,
          accuracy: pos.coords.accuracy,
          speed: speedKmh,
          heading: pos.coords.heading || 0,
          timestamp: pos.timestamp
        });
      },
      err => console.error('GPS Watch error:', err),
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 1000
      }
    );
  }

  stopTracking(): void {
    if (this.watchId !== null) {
      navigator.geolocation.clearWatch(this.watchId);
      this.watchId = null;
    }
  }

  // Off-route distance detection (cross-track distance)
  isOffRoute(currentLat: number, currentLng: number, routePolyline: [number, number][], thresholdMeters: number = 35.0): boolean {
    if (!routePolyline || routePolyline.length < 2) return false;
    let minDistance = Infinity;

    for (let i = 0; i < routePolyline.length - 1; i++) {
      const p1 = routePolyline[i];
      const p2 = routePolyline[i + 1];
      const d = this.distToSegmentMeters(currentLat, currentLng, p1[0], p1[1], p2[0], p2[1]);
      if (d < minDistance) minDistance = d;
    }

    return minDistance > thresholdMeters;
  }

  private calculateDistanceMeters(lat1: number, lon1: number, lat2: number, lon2: number): number {
    const R = 6371e3;
    const phi1 = (lat1 * Math.PI) / 180;
    const phi2 = (lat2 * Math.PI) / 180;
    const dPhi = ((lat2 - lat1) * Math.PI) / 180;
    const dLambda = ((lon2 - lon1) * Math.PI) / 180;

    const a = Math.sin(dPhi / 2) * Math.sin(dPhi / 2) +
              Math.cos(phi1) * Math.cos(phi2) * Math.sin(dLambda / 2) * Math.sin(dLambda / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  }

  private distToSegmentMeters(px: number, py: number, x1: number, y1: number, x2: number, y2: number): number {
    const l2 = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);
    if (l2 === 0) return this.calculateDistanceMeters(px, py, x1, y1);
    let t = ((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / l2;
    t = Math.max(0, Math.min(1, t));
    const projX = x1 + t * (x2 - x1);
    const projY = y1 + t * (y2 - y1);
    return this.calculateDistanceMeters(px, py, projX, projY);
  }
}
