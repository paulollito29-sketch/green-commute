// Smart Location Search Service for EcoCommute
// Features: Local proximity biasing, multi-source geocoding (Photon + Nominatim), 
// category matching (Parks, Universities, Workplaces, Bike Hubs, Stations), search history, and instant fallbacks.

class SmartSearchService {
  constructor() {
    this.searchHistoryKey = 'ecocommute_search_history';
    this.cachedResults = new Map();
    this.defaultOrigin = { lat: -12.0897, lng: -77.0543 }; // Lima center fallback

    // Curated Eco & Popular Points of Interest for instant matches
    this.smartPOIs = [
      { name: 'Parque Kennedy / Miraflores', category: 'park', icon: '🌳', lat: -12.1215, lng: -77.0298, desc: 'Espacio verde y corredor ciclista' },
      { name: 'Centro Financiero / San Isidro', category: 'work', icon: '🏢', lat: -12.0965, lng: -77.0285, desc: 'Zona empresarial de alta demanda' },
      { name: 'Universidad Central del Perú', category: 'education', icon: '🎓', lat: -12.0820, lng: -77.0490, desc: 'Campus universitario con ciclovía' },
      { name: 'Malecón de la Costa Verde', category: 'eco', icon: '🌊', lat: -12.1350, lng: -77.0320, desc: 'Corredor ecológico costero' },
      { name: 'Estación Central Metropolitano', category: 'station', icon: '🚇', lat: -12.0550, lng: -77.0350, desc: 'Hub intermodal de transporte' },
      { name: 'Ciclovía Av. Arequipa', category: 'bike', icon: '🚲', lat: -12.0800, lng: -77.0360, desc: 'Eje principal de movilidad sostenible' },
      { name: 'Parque de la Reserva (Circuito Mágico)', category: 'park', icon: '🌳', lat: -12.0690, lng: -77.0335, desc: 'Parque urbano y zona peatonal' },
      { name: 'Centro Histórico / Plaza Mayor', category: 'culture', icon: '🏛️', lat: -12.0463, lng: -77.0305, desc: 'Casco histórico peatonalizado' },
      { name: 'Centro Comercial Larcomar', category: 'shop', icon: '🛍️', lat: -12.1315, lng: -77.0305, desc: 'Punto de interés turístico y comercial' },
      { name: 'Jardín Botánico & Bosque El Olivar', category: 'eco', icon: '🌿', lat: -12.1010, lng: -77.0365, desc: 'Reserva natural y pulmón verde urbano' }
    ];
  }

  getHistory() {
    try {
      const raw = localStorage.getItem(this.searchHistoryKey);
      return raw ? JSON.parse(raw) : [];
    } catch (_) {
      return [];
    }
  }

  saveToHistory(item) {
    if (!item || !item.name) return;
    try {
      let history = this.getHistory();
      history = history.filter(h => h.name.toLowerCase() !== item.name.toLowerCase());
      history.unshift({
        name: item.name,
        lat: item.lat,
        lng: item.lng,
        icon: item.icon || '📍',
        desc: item.desc || 'Búsqueda reciente',
        timestamp: Date.now()
      });
      history = history.slice(0, 5); // Keep top 5
      localStorage.setItem(this.searchHistoryKey, JSON.stringify(history));
    } catch (_) {}
  }

  clearHistory() {
    try {
      localStorage.removeItem(this.searchHistoryKey);
    } catch (_) {}
  }

  /**
   * Búsqueda inteligente de ubicaciones con autocompletado en tiempo real
   * @param {string} query Texto buscado
   * @param {Object} userLocation Ubicación actual del usuario para sesgo geográfico
   * @returns {Promise<Array>} Lista de sugerencias categorizadas
   */
  async search(query, userLocation = null) {
    const trimmed = (query || '').trim();
    const loc = userLocation || this.defaultOrigin;

    if (!trimmed) {
      // Retornar búsquedas recientes + POIs destacados si está vacío
      const history = this.getHistory();
      return [
        ...history.map(h => ({ ...h, isRecent: true })),
        ...this.smartPOIs.slice(0, 4).map(p => ({ ...p, isFeatured: true }))
      ];
    }

    const cacheKey = `${trimmed.toLowerCase()}_${loc.lat.toFixed(2)}_${loc.lng.toFixed(2)}`;
    if (this.cachedResults.has(cacheKey)) {
      return this.cachedResults.get(cacheKey);
    }

    // 1. Coincidencias locales instantáneas en POIs
    const localMatches = this.smartPOIs.filter(poi => 
      poi.name.toLowerCase().includes(trimmed.toLowerCase()) ||
      poi.desc.toLowerCase().includes(trimmed.toLowerCase()) ||
      poi.category.toLowerCase().includes(trimmed.toLowerCase())
    ).map(poi => ({
      ...poi,
      distanceKm: this.calculateDistanceKm(loc.lat, loc.lng, poi.lat, poi.lng)
    }));

    // 2. Consulta a Photon API (OpenStreetMap geocoder con sesgo por proximidad)
    let remoteMatches = [];
    try {
      const photonUrl = `https://photon.komoot.io/api/?q=${encodeURIComponent(trimmed)}&lat=${loc.lat}&lon=${loc.lng}&limit=6`;
      const res = await fetch(photonUrl);
      if (res.ok) {
        const data = await res.json();
        if (data.features && data.features.length > 0) {
          remoteMatches = data.features.map(f => {
            const props = f.properties || {};
            const coords = f.geometry.coordinates; // [lng, lat]
            const name = [props.name, props.street, props.city || props.district].filter(Boolean).join(', ') || props.name || 'Ubicación';
            const category = this.detectCategory(props);
            
            return {
              name: name,
              lat: coords[1],
              lng: coords[0],
              icon: category.icon,
              category: category.type,
              desc: [props.district, props.city, props.country].filter(Boolean).join(' • ') || 'Resultado en mapa',
              distanceKm: this.calculateDistanceKm(loc.lat, loc.lng, coords[1], coords[0])
            };
          });
        }
      }
    } catch (_) {
      // Fallback a Nominatim si Photon falla
      try {
        const nomUrl = `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(trimmed)}&limit=5&lat=${loc.lat}&lon=${loc.lng}`;
        const res = await fetch(nomUrl);
        if (res.ok) {
          const data = await res.json();
          remoteMatches = data.map(item => ({
            name: item.display_name.split(',').slice(0, 2).join(','),
            lat: parseFloat(item.lat),
            lng: parseFloat(item.lon),
            icon: '📍',
            category: 'place',
            desc: item.display_name.split(',').slice(2, 4).join(', '),
            distanceKm: this.calculateDistanceKm(loc.lat, loc.lng, parseFloat(item.lat), parseFloat(item.lon))
          }));
        }
      } catch (__) {}
    }

    // Unir, deduplicar por nombre y ordenar por distancia
    const combined = [...localMatches, ...remoteMatches];
    const unique = [];
    const seen = new Set();

    for (const item of combined) {
      const norm = item.name.toLowerCase().trim();
      if (!seen.has(norm)) {
        seen.add(norm);
        unique.push(item);
      }
    }

    // Ordenar con prioridad a los más cercanos
    unique.sort((a, b) => (a.distanceKm || 999) - (b.distanceKm || 999));
    const finalResults = unique.slice(0, 7);

    this.cachedResults.set(cacheKey, finalResults);
    return finalResults;
  }

  detectCategory(props) {
    const osmKey = props.osm_key || '';
    const osmVal = props.osm_value || '';
    const type = props.type || '';

    if (osmKey === 'leisure' || osmVal === 'park' || osmVal === 'garden') {
      return { type: 'park', icon: '🌳' };
    }
    if (osmKey === 'amenity' && (osmVal === 'university' || osmVal === 'college' || osmVal === 'school')) {
      return { type: 'education', icon: '🎓' };
    }
    if (osmKey === 'amenity' && (osmVal === 'cafe' || osmVal === 'restaurant' || osmVal === 'fast_food')) {
      return { type: 'food', icon: '☕' };
    }
    if (osmKey === 'building' && (osmVal === 'commercial' || osmVal === 'office')) {
      return { type: 'work', icon: '🏢' };
    }
    if (osmKey === 'railway' || osmVal === 'station' || osmVal === 'subway') {
      return { type: 'station', icon: '🚇' };
    }
    if (osmKey === 'highway' && osmVal === 'cycleway') {
      return { type: 'bike', icon: '🚲' };
    }
    return { type: 'place', icon: '📍' };
  }

  calculateDistanceKm(lat1, lon1, lat2, lon2) {
    const R = 6371;
    const dLat = (lat2 - lat1) * (Math.PI / 180);
    const dLon = (lon2 - lon1) * (Math.PI / 180);
    const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
              Math.cos(lat1 * (Math.PI / 180)) * Math.cos(lat2 * (Math.PI / 180)) *
              Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return Math.round(R * c * 10) / 10;
  }
}

window.smartSearchService = new SmartSearchService();
