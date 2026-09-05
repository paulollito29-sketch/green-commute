import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, map, catchError } from 'rxjs';

export interface SearchResult {
  name: string;
  lat: number;
  lng: number;
  category: string;
  icon: string;
  distanceKm?: number;
}

@Injectable({
  providedIn: 'root'
})
export class SmartSearchService {
  constructor(private http: HttpClient) {}

  search(query: string, userLat?: number, userLng?: number): Observable<SearchResult[]> {
    if (!query || query.trim().length < 2) return of([]);

    const encoded = encodeURIComponent(query.trim());
    let url = `https://photon.komoot.io/api/?q=${encoded}&limit=8`;
    if (userLat && userLng) {
      url += `&lat=${userLat}&lon=${userLng}`;
    }

    return this.http.get<any>(url).pipe(
      map(res => {
        if (!res || !res.features) return [];
        return res.features.map((f: any) => {
          const coords = f.geometry.coordinates; // [lon, lat]
          const props = f.properties || {};
          const name = props.name || props.street || query;
          const city = props.city || props.district || props.country || '';
          const fullName = city ? `${name}, ${city}` : name;

          return {
            name: fullName,
            lat: coords[1],
            lng: coords[0],
            category: props.osm_value || 'lugar',
            icon: '📍'
          };
        });
      }),
      catchError(() => of([]))
    );
  }
}
