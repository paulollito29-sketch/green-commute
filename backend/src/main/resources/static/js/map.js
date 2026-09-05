// Geolocation Tracking, Dynamic Off-Route Detection & Waze-Style Live Navigation
class GeolocationTrackingService {
  constructor() {
    this.minDistanceMetersFilter = 3.0;
    this.lastEmittedPoint = null;
    this.watchId = null;
    this.listeners = [];
  }

  startTracking(onPointCallback, onErrorCallback) {
    if (!('geolocation' in navigator)) {
      if (onErrorCallback) onErrorCallback(new Error('Geolocalización no soportada'));
      return;
    }

    this.stopTracking();

    this.watchId = navigator.geolocation.watchPosition(
      (pos) => {
        const currentPoint = {
          latitude: pos.coords.latitude,
          longitude: pos.coords.longitude,
          altitude: pos.coords.altitude,
          speedKmh: pos.coords.speed && pos.coords.speed > 0 ? Math.round(pos.coords.speed * 3.6) : 0,
          heading: pos.coords.heading || null,
          timestamp: pos.timestamp
        };

        if (!this.lastEmittedPoint) {
          this.lastEmittedPoint = currentPoint;
          onPointCallback(currentPoint);
          return;
        }

        const distMeters = this.calculateDistanceMeters(
          this.lastEmittedPoint.latitude, this.lastEmittedPoint.longitude,
          currentPoint.latitude, currentPoint.longitude
        );

        if (distMeters >= this.minDistanceMetersFilter) {
          this.lastEmittedPoint = currentPoint;
          onPointCallback(currentPoint);
        }
      },
      (err) => {
        if (onErrorCallback) onErrorCallback(err);
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 1500
      }
    );
  }

  stopTracking() {
    if (this.watchId !== null) {
      navigator.geolocation.clearWatch(this.watchId);
      this.watchId = null;
    }
    this.lastEmittedPoint = null;
  }

  /**
   * Detección de Desvío (Off-Route Detection):
   * Calcula la distancia perpendicular mínima en metros desde el punto GPS actual hacia la polilínea activa.
   */
  isOffRoute(currentPoint, polyline, thresholdMeters = 20.0) {
    if (!polyline || polyline.length < 2) return false;

    let minDistanceMeters = Infinity;
    for (let i = 0; i < polyline.length - 1; i++) {
      const p1 = polyline[i];
      const p2 = polyline[i + 1];
      const dist = this.pointToSegmentDistanceMeters(
        currentPoint.latitude, currentPoint.longitude,
        p1[0], p1[1],
        p2[0], p2[1]
      );
      if (dist < minDistanceMeters) {
        minDistanceMeters = dist;
      }
    }

    return minDistanceMeters > thresholdMeters;
  }

  calculateDistanceMeters(lat1, lon1, lat2, lon2) {
    const R = 6371000;
    const dLat = (lat2 - lat1) * (Math.PI / 180);
    const dLon = (lon2 - lon1) * (Math.PI / 180);
    const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
              Math.cos(lat1 * (Math.PI / 180)) * Math.cos(lat2 * (Math.PI / 180)) *
              Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  }

  pointToSegmentDistanceMeters(pLat, pLng, aLat, aLng, bLat, bLng) {
    const latFactor = 111139.0;
    const lngFactor = 111139.0 * Math.cos(aLat * (Math.PI / 180));

    const px = (pLng - aLng) * lngFactor;
    const py = (pLat - aLat) * latFactor;
    const bx = (bLng - aLng) * lngFactor;
    const by = (bLat - aLat) * latFactor;

    const segmentLengthSq = bx * bx + by * by;
    if (segmentLengthSq === 0) return Math.hypot(px, py);

    const t = Math.max(0, Math.min(1, (px * bx + py * by) / segmentLengthSq));
    const projX = t * bx;
    const projY = t * by;

    return Math.hypot(px - projX, py - projY);
  }
}

class MapManager {
  constructor() {
    this.map = null;
    this.userMarker = null;
    this.navMarker = null;
    this.destMarker = null;
    this.routePolylines = [];
    this.currentPosition = { lat: -12.0897, lng: -77.0543, name: 'Mi Ubicación (GPS)' };
    this.destinationPosition = null;

    // Tracking Service
    this.trackingService = new GeolocationTrackingService();

    // Waze Live Navigation State
    this.isNavigating = false;
    this.isPlaying = false;
    this.simulationSpeedMultiplier = 1;
    this.navInterval = null;
    this.activeRoute = null;
    this.navWaypoints = [];
    this.currentWaypointIndex = 0;
    this.currentHeading = 0;
    this.autoFollowCamera = true;
    this.voiceEnabled = true;
    this.lastSpokenKey = '';
    this.accumulatedCo2SavedGrams = 0.0;
    this.lastTelemetryLocation = null;
    this.maneuverPoints = [];

    // Callbacks
    this.onNavUpdate = null;
    this.onNavFinished = null;
    this.onGpsLocked = null;
    this.onOffRouteDetected = null;
  }

  init(containerId = 'map') {
    this.map = L.map(containerId, {
      zoomControl: false,
      attributionControl: false
    }).setView([this.currentPosition.lat, this.currentPosition.lng], 15);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      subdomains: ['a', 'b', 'c']
    }).addTo(this.map);

    L.control.zoom({ position: 'bottomright' }).addTo(this.map);

    const gpsIcon = L.divIcon({
      className: 'gps-pulse-marker',
      iconSize: [22, 22],
      iconAnchor: [11, 11]
    });

    this.userMarker = L.marker([this.currentPosition.lat, this.currentPosition.lng], {
      icon: gpsIcon,
      zIndexOffset: 1000
    }).addTo(this.map);

    this.map.on('dragstart', () => {
      if (this.isNavigating) {
        this.autoFollowCamera = false;
        const btnRecenter = document.getElementById('btnRecenterNav');
        if (btnRecenter) {
          btnRecenter.classList.remove('hidden');
          btnRecenter.classList.add('flex');
        }
      }
    });

    this.map.on('click', async (e) => {
      if (this.isNavigating) return;

      const lat = e.latlng.lat;
      const lng = e.latlng.lng;
      let name = `Punto en mapa (${lat.toFixed(4)}, ${lng.toFixed(4)})`;

      try {
        const geoRes = await fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}&zoom=16`);
        if (geoRes.ok) {
          const data = await geoRes.json();
          if (data.display_name) {
            name = data.display_name.split(',').slice(0, 2).join(',');
          }
        }
      } catch (_) {}

      this.setDestination(lat, lng, name);
      if (window.app) {
        window.app.calculateCurrentRoute(false);
      }
    });

    this.startBackgroundGpsWatch();
  }

  startBackgroundGpsWatch() {
    this.trackingService.startTracking((point) => {
      this.currentPosition.lat = point.latitude;
      this.currentPosition.lng = point.longitude;

      if (this.userMarker) {
        this.userMarker.setLatLng([point.latitude, point.longitude]);
      }

      if (!this.isNavigating) {
        const inputOrigin = document.getElementById('inputOrigin');
        if (inputOrigin) {
          inputOrigin.value = this.currentPosition.name || `Mi Ubicación (${point.latitude.toFixed(4)}, ${point.longitude.toFixed(4)})`;
        }
      } else {
        // En navegación activa: comprobar desvío de 20m y telemetría
        this.handleActiveNavigationGpsTick(point);
      }

      if (this.onGpsLocked) {
        this.onGpsLocked(this.currentPosition);
      }
    });
  }

  handleActiveNavigationGpsTick(point) {
    if (!this.activeRoute || !this.activeRoute.pathCoordinates) return;

    // 1. Off-Route Check (> 20 metros)
    const isOff = this.trackingService.isOffRoute(point, this.activeRoute.pathCoordinates, 20.0);
    if (isOff && this.onOffRouteDetected) {
      this.onOffRouteDetected(point);
      return;
    }

    // 2. Continuous Telemetry Tick if moved > 5m
    if (this.lastTelemetryLocation) {
      const incrementMeters = this.trackingService.calculateDistanceMeters(
        this.lastTelemetryLocation.latitude, this.lastTelemetryLocation.longitude,
        point.latitude, point.longitude
      );

      if (incrementMeters >= 5.0) {
        window.api.recordTelemetryTick({
          tripId: this.activeRoute.tripId || '00000000-0000-0000-0000-000000000000',
          currentLat: point.latitude,
          currentLng: point.longitude,
          speedKmh: point.speedKmh || 0,
          heading: point.heading || this.currentHeading,
          distanceIncrementMeters: incrementMeters,
          elapsedSeconds: 3
        }).then((res) => {
          if (res && res.totalCo2SavedGrams) {
            this.accumulatedCo2SavedGrams += res.totalCo2SavedGrams;
          }
        }).catch(() => {});
      }
    }
    this.lastTelemetryLocation = point;

    if (!this.isPlaying) {
      this.handleRealGpsMove(point.latitude, point.longitude);
    }
  }

  setDestination(lat, lng, name = 'Destino Seleccionado') {
    this.destinationPosition = { lat, lng, name };

    if (this.destMarker) {
      this.map.removeLayer(this.destMarker);
    }

    const destIcon = L.divIcon({
      html: `<div class="w-9 h-9 rounded-full bg-rose-600 text-white flex items-center justify-center shadow-xl shadow-rose-600/50 border-2 border-white text-xs font-black ring-4 ring-rose-500/20">🏁</div>`,
      className: '',
      iconSize: [36, 36],
      iconAnchor: [18, 36]
    });

    this.destMarker = L.marker([lat, lng], { icon: destIcon }).addTo(this.map);
    this.destMarker.bindPopup(`<div class="text-xs font-bold text-slate-800 p-1"><b>${name}</b><br><span class="text-[10px] text-emerald-600 font-semibold">Destino Fijado</span></div>`).openPopup();

    const inputDest = document.getElementById('inputDestination');
    if (inputDest) {
      inputDest.value = name;
    }
  }

  clearRoutes() {
    this.routePolylines.forEach(layer => this.map.removeLayer(layer));
    this.routePolylines = [];
  }

  drawRoutes(routes, selectedOptionId = null) {
    this.clearRoutes();
    if (!routes || routes.length === 0) return;

    let selectedRoute = null;

    routes.forEach(route => {
      const isSelected = selectedOptionId ? route.id === selectedOptionId : route.isAiRecommended;
      if (isSelected) selectedRoute = route;

      const isAi = route.isAiRecommended;
      const color = isSelected ? (isAi ? '#10b981' : '#38bdf8') : '#475569';
      const weight = isSelected ? 6 : 4;
      const opacity = isSelected ? 0.95 : 0.45;

      const polyline = L.polyline(route.pathCoordinates, {
        color: color,
        weight: weight,
        opacity: opacity,
        lineCap: 'round',
        lineJoin: 'round'
      }).addTo(this.map);

      polyline.on('click', () => {
        if (window.app && !this.isNavigating) {
          window.app.selectRouteOption(route.id);
        }
      });

      this.routePolylines.push(polyline);
    });

    if (selectedRoute && selectedRoute.pathCoordinates && selectedRoute.pathCoordinates.length > 0 && !this.isNavigating) {
      const bounds = L.latLngBounds(selectedRoute.pathCoordinates);
      this.map.fitBounds(bounds, { padding: [60, 60], maxZoom: 16 });
    }
  }

  startLiveNavigation(route, autoPlay = true, speedMultiplier = 1) {
    this.isNavigating = true;
    this.isPlaying = autoPlay;
    this.simulationSpeedMultiplier = speedMultiplier;
    this.activeRoute = route;
    this.autoFollowCamera = true;
    this.currentWaypointIndex = 0;
    this.lastSpokenKey = '';
    this.accumulatedCo2SavedGrams = 0.0;
    this.lastTelemetryLocation = null;

    // Extraer o generar waypoints de alta frecuencia
    this.navWaypoints = this.interpolateWaypoints(route.pathCoordinates, 75);
    this.maneuverPoints = this.calculateManeuverSchedule(this.navWaypoints, route);

    if (this.navMarker) {
      this.map.removeLayer(this.navMarker);
    }

    const startCoord = this.navWaypoints[0];
    const navDivIcon = L.divIcon({
      className: '',
      html: `
        <div id="wazeNavMarkerElement" class="waze-nav-marker" style="transform: rotate(0deg);">
          <div class="waze-nav-arrow"></div>
        </div>
      `,
      iconSize: [36, 36],
      iconAnchor: [18, 18]
    });

    this.navMarker = L.marker([startCoord[0], startCoord[1]], {
      icon: navDivIcon,
      zIndexOffset: 2000
    }).addTo(this.map);

    this.map.setView([startCoord[0], startCoord[1]], 17, { animate: true });

    // Show Waze Speedometer & Top Banner
    document.getElementById('wazeSpeedometer')?.classList.remove('hidden');
    document.getElementById('topTurnBanner')?.classList.remove('hidden');

    this.speak(`Iniciando recorrido en ${route.modeDisplayName}. Destino a ${route.distanceKm} kilómetros.`);
    this.updateNavigationState();

    if (autoPlay) {
      this.playNavigation();
    }
  }

  prepareLiveNavigation(route) {
    this.startLiveNavigation(route, true, this.simulationSpeedMultiplier);
  }

  updateRouteOnRecalculate(newRoute) {
    this.activeRoute = newRoute;
    this.navWaypoints = this.interpolateWaypoints(newRoute.pathCoordinates, 75);
    this.maneuverPoints = this.calculateManeuverSchedule(this.navWaypoints, newRoute);
    this.currentWaypointIndex = 0;
    this.clearRoutes();
    this.drawRoutes([newRoute], newRoute.id);
    this.speak("Ruta recalculada por desvío de trayectoria.");
    this.updateNavigationState();
  }

  togglePlayPause() {
    if (this.isPlaying) {
      this.pauseNavigation();
    } else {
      this.playNavigation();
    }
    return this.isPlaying;
  }

  playNavigation() {
    this.isPlaying = true;
    clearInterval(this.navInterval);

    const tickIntervalMs = 400;
    this.navInterval = setInterval(() => {
      if (!this.isPlaying || !this.isNavigating) return;

      if (this.currentWaypointIndex >= this.navWaypoints.length - 1) {
        this.finishLiveNavigation();
        return;
      }

      this.currentWaypointIndex++;
      this.updateNavigationState();
    }, Math.max(80, Math.round(tickIntervalMs / this.simulationSpeedMultiplier)));
  }

  pauseNavigation() {
    this.isPlaying = false;
    clearInterval(this.navInterval);
  }

  updateNavigationState() {
    if (!this.navWaypoints || this.navWaypoints.length === 0) return;

    const currentCoord = this.navWaypoints[this.currentWaypointIndex];
    const prevCoord = this.navWaypoints[Math.max(0, this.currentWaypointIndex - 1)];
    const nextCoord = this.navWaypoints[Math.min(this.navWaypoints.length - 1, this.currentWaypointIndex + 2)];

    const heading = this.calculateBearing(prevCoord[0], prevCoord[1], currentCoord[0], currentCoord[1]);
    this.currentHeading = heading;

    // 1. Waze Course-Up Map Rotation
    const mapEl = document.getElementById('map');
    if (mapEl && this.isNavigating && this.autoFollowCamera) {
      mapEl.style.transform = `rotate(${-heading}deg)`;
      mapEl.style.transformOrigin = 'center center';
    }

    // 2. Nav Marker Position & Counter-Rotation
    if (this.navMarker) {
      this.navMarker.setLatLng([currentCoord[0], currentCoord[1]]);
      const markerEl = document.getElementById('wazeNavMarkerElement');
      if (markerEl) {
        markerEl.style.transform = `rotate(${heading}deg)`;
      }
    }

    // 3. Camera Pan Centered on vehicle
    if (this.autoFollowCamera && this.map) {
      this.map.panTo([currentCoord[0], currentCoord[1]], { animate: true, duration: 0.35 });
    }

    // 4. Speedometer & Mode Metrics
    let baseSpeedKmh = 18.0;
    if (this.activeRoute.mode === 'WALKING') baseSpeedKmh = 4.8;
    else if (this.activeRoute.mode === 'BICYCLE') baseSpeedKmh = 18.5;
    else baseSpeedKmh = 35.0;

    const currentSpeed = this.isPlaying ? Math.round(baseSpeedKmh * (0.92 + Math.random() * 0.16)) : 0;
    const progressPercent = Math.round((this.currentWaypointIndex / (this.navWaypoints.length - 1)) * 100);
    const remainingDistanceKm = Math.max(0, this.activeRoute.distanceKm * (1 - (this.currentWaypointIndex / this.navWaypoints.length)));
    const remainingMinutes = Math.max(1, Math.round((remainingDistanceKm / (baseSpeedKmh || 15)) * 60));
    const currentCo2Saved = (this.activeRoute.co2SavedGrams * (progressPercent / 100)).toFixed(1);

    // Update Floating Speedometer in UI
    const spdVal = document.getElementById('speedometerValue');
    if (spdVal) spdVal.innerText = currentSpeed;

    const spdIcon = document.getElementById('speedometerModeIcon');
    const spdText = document.getElementById('speedometerModeText');
    if (spdIcon && spdText) {
      spdIcon.innerText = this.activeRoute.mode === 'WALKING' ? '🚶' : (this.activeRoute.mode === 'BICYCLE' ? '🚲' : '🚗');
      spdText.innerText = this.activeRoute.modeDisplayName;
    }

    // 5. Dynamic Next Turn Countdown & Maneuver Analysis
    const maneuver = this.findNextManeuver(this.currentWaypointIndex, currentCoord);

    // Update Waze Top Turn Banner
    const bannerMeters = document.getElementById('turnCountdownMeters');
    const bannerUnit = document.getElementById('turnCountdownUnit');
    const bannerIcon = document.getElementById('turnInstructionIcon');
    const bannerText = document.getElementById('turnInstructionText');
    const bannerNext = document.getElementById('turnNextStepPreview');
    const bannerEta = document.getElementById('turnInstructionEta');

    if (bannerMeters && bannerUnit) {
      if (maneuver.distanceMeters >= 1000) {
        bannerMeters.innerText = (maneuver.distanceMeters / 1000).toFixed(1);
        bannerUnit.innerText = 'km';
      } else {
        bannerMeters.innerText = Math.round(maneuver.distanceMeters);
        bannerUnit.innerText = 'm';
      }
    }

    if (bannerIcon) bannerIcon.innerText = maneuver.icon;
    if (bannerText) bannerText.innerText = maneuver.text;
    if (bannerNext) bannerNext.innerText = maneuver.nextPreviewText;
    if (bannerEta) bannerEta.innerText = `${remainingMinutes} min (${remainingDistanceKm.toFixed(1)} km)`;

    // 6. Proactive Voice Guidance (Trigger at 150m and 30m)
    if (this.isPlaying && maneuver.speakKey && maneuver.speakKey !== this.lastSpokenKey) {
      if (maneuver.distanceMeters <= 160 && maneuver.distanceMeters >= 30) {
        this.speak(`En ${Math.round(maneuver.distanceMeters)} metros, ${maneuver.actionText}`);
        this.lastSpokenKey = maneuver.speakKey;
      } else if (maneuver.distanceMeters < 30) {
        this.speak(maneuver.actionText);
        this.lastSpokenKey = maneuver.speakKey + '_now';
      }
    }

    if (this.onNavUpdate) {
      this.onNavUpdate({
        progressPercent,
        remainingDistanceKm: remainingDistanceKm.toFixed(2),
        remainingMinutes,
        currentSpeed,
        currentCo2SavedGrams: currentCo2Saved,
        nextInstruction: maneuver,
        currentCoord,
        isPlaying: this.isPlaying
      });
    }
  }

  handleRealGpsMove(lat, lng) {
    if (!this.navWaypoints || this.navWaypoints.length === 0 || !this.destinationPosition) return;

    // 1. Proximidad al punto de llegada (<= 30 metros)
    const distToDestMeters = this.trackingService.calculateDistanceMeters(
      lat, lng,
      this.destinationPosition.lat, this.destinationPosition.lng
    );

    if (distToDestMeters <= 30.0) {
      console.log('🏁 Llegada al destino detectada por proximidad GPS (< 30m)');
      this.finishLiveNavigation();
      return;
    }

    // 2. Encontrar waypoint más cercano en la ruta activa
    let closestIdx = 0;
    let minDistanceMeters = 999999;
    for (let i = 0; i < this.navWaypoints.length; i++) {
      const pt = this.navWaypoints[i];
      const dMeters = this.trackingService.calculateDistanceMeters(pt[0], pt[1], lat, lng);
      if (dMeters < minDistanceMeters) {
        minDistanceMeters = dMeters;
        closestIdx = i;
      }
    }

    // 3. Si se desvía más de 20 metros de la ruta activa, disparar recálculo automático
    if (minDistanceMeters > 20.0 && this.onOffRouteDetected) {
      console.warn(`⚠️ Desvío detectado (${minDistanceMeters.toFixed(1)}m de la ruta). Recalculando...`);
      this.onOffRouteDetected({ latitude: lat, longitude: lng });
      return;
    }

    this.currentWaypointIndex = closestIdx;
    this.updateNavigationState();
  }

  calculateManeuverSchedule(waypoints, route) {
    const list = [];
    if (!waypoints || waypoints.length < 3) return list;

    for (let i = 2; i < waypoints.length - 2; i++) {
      const p1 = waypoints[i - 2];
      const p2 = waypoints[i];
      const p3 = waypoints[i + 2];

      const b1 = this.calculateBearing(p1[0], p1[1], p2[0], p2[1]);
      const b2 = this.calculateBearing(p2[0], p2[1], p3[0], p3[1]);
      const diff = (b2 - b1 + 180) % 360 - 180;

      if (Math.abs(diff) > 35) {
        const isRight = diff > 0;
        list.push({
          waypointIndex: i,
          coord: p2,
          icon: isRight ? '➡️' : '⬅️',
          actionText: isRight ? 'gira a la derecha' : 'gira a la izquierda',
          text: isRight ? 'Gira a la derecha en la intersección' : 'Gira a la izquierda en la intersección',
          speakKey: `turn_${i}_${isRight ? 'R' : 'L'}`
        });
        i += 6; // Espaciar giros detectados
      }
    }

    // Destino final
    list.push({
      waypointIndex: waypoints.length - 1,
      coord: waypoints[waypoints.length - 1],
      icon: '🏁',
      actionText: 'has llegado a tu destino',
      text: '¡Destino alcanzado!',
      speakKey: 'arrive_final'
    });

    return list;
  }

  findNextManeuver(currentIndex, currentCoord) {
    const next = this.maneuverPoints.find(m => m.waypointIndex >= currentIndex);
    if (!next) {
      return {
        distanceMeters: 0,
        icon: '🏁',
        text: 'Llegando a tu destino',
        actionText: 'has llegado a tu destino',
        nextPreviewText: 'Fin del recorrido sostenible',
        speakKey: null
      };
    }

    const distMeters = this.trackingService.calculateDistanceMeters(
      currentCoord[0], currentCoord[1],
      next.coord[0], next.coord[1]
    );

    // Próximo paso secundario
    const subsequent = this.maneuverPoints.find(m => m.waypointIndex > next.waypointIndex);
    const nextPreviewText = subsequent ? `Luego: ${subsequent.text}` : 'Luego: Llegada a meta';

    return {
      distanceMeters: distMeters,
      icon: next.icon,
      text: next.text,
      actionText: next.actionText,
      nextPreviewText,
      speakKey: next.speakKey
    };
  }

  speak(text) {
    if (!this.voiceEnabled || !('speechSynthesis' in window) || !text) return;
    try {
      window.speechSynthesis.cancel();
      const utterance = new SpeechSynthesisUtterance(text);
      utterance.lang = 'es-ES';
      utterance.rate = 1.05;
      window.speechSynthesis.speak(utterance);
    } catch (_) {}
  }

  toggleVoice() {
    this.voiceEnabled = !this.voiceEnabled;
    const btn = document.getElementById('btnToggleVoice');
    if (btn) {
      btn.innerText = this.voiceEnabled ? '🔊' : '🔇';
      btn.title = this.voiceEnabled ? 'Voz Activada' : 'Voz Silenciada';
    }
    return this.voiceEnabled;
  }

  setSimulationSpeed(speed) {
    this.simulationSpeedMultiplier = speed;
    if (this.isPlaying) {
      this.playNavigation();
    }
  }

  recenterNavigation() {
    this.autoFollowCamera = true;
    const btnRecenter = document.getElementById('btnRecenterNav');
    if (btnRecenter) {
      btnRecenter.classList.add('hidden');
      btnRecenter.classList.remove('flex');
    }

    if (this.navMarker && this.map) {
      const pos = this.navMarker.getLatLng();
      this.map.setView(pos, 17, { animate: true });
    }
  }

  stopLiveNavigation() {
    this.isNavigating = false;
    this.isPlaying = false;
    clearInterval(this.navInterval);

    // Reset Map Rotation
    const mapEl = document.getElementById('map');
    if (mapEl) {
      mapEl.style.transform = 'none';
    }

    if (this.navMarker) {
      this.map.removeLayer(this.navMarker);
      this.navMarker = null;
    }

    document.getElementById('wazeSpeedometer')?.classList.add('hidden');
    document.getElementById('topTurnBanner')?.classList.add('hidden');

    this.autoFollowCamera = true;
    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel();
    }
  }

  finishLiveNavigation() {
    this.speak("¡Has llegado a tu destino! Felicitaciones.");
    this.stopLiveNavigation();
    if (this.onNavFinished && this.activeRoute) {
      this.onNavFinished(this.activeRoute);
    }
  }

  calculateBearing(lat1, lon1, lat2, lon2) {
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const y = Math.sin(dLon) * Math.cos(lat2 * Math.PI / 180);
    const x = Math.cos(lat1 * Math.PI / 180) * Math.sin(lat2 * Math.PI / 180) -
              Math.sin(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * Math.cos(dLon);
    let brng = Math.atan2(y, x) * 180 / Math.PI;
    return (brng + 360) % 360;
  }

  interpolateWaypoints(coords, targetCount = 75) {
    if (!coords || coords.length === 0) return [];
    if (coords.length >= targetCount) return coords;

    const interpolated = [];
    for (let i = 0; i < coords.length - 1; i++) {
      const p1 = coords[i];
      const p2 = coords[i + 1];
      const stepsBetween = Math.ceil(targetCount / coords.length);
      for (let s = 0; s < stepsBetween; s++) {
        const t = s / stepsBetween;
        const lat = p1[0] + (p2[0] - p1[0]) * t;
        const lng = p1[1] + (p2[1] - p1[1]) * t;
        interpolated.push([lat, lng]);
      }
    }
    interpolated.push(coords[coords.length - 1]);
    return interpolated;
  }

  recenter() {
    if (this.currentPosition && this.map) {
      this.map.setView([this.currentPosition.lat, this.currentPosition.lng], 16, { animate: true });
    }
  }
}

window.mapManager = new MapManager();
