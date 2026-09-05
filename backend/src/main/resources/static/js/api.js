// API Client for EcoCommute Spring Boot Backend
const API_BASE = '/api/v1';

class EcoCommuteApi {
  constructor() {
    this.token = localStorage.getItem('ecocommute_jwt') || null;
  }

  setToken(token) {
    this.token = token;
    if (token) {
      localStorage.setItem('ecocommute_jwt', token);
    } else {
      localStorage.removeItem('ecocommute_jwt');
    }
  }

  getHeaders() {
    const headers = { 'Content-Type': 'application/json' };
    if (this.token) {
      headers['Authorization'] = `Bearer ${this.token}`;
    }
    return headers;
  }

  async request(endpoint, options = {}) {
    const url = `${API_BASE}${endpoint}`;
    const config = {
      ...options,
      headers: {
        ...this.getHeaders(),
        ...(options.headers || {})
      }
    };

    try {
      const response = await fetch(url, config);
      if (response.status === 401) {
        // Expired or invalid token
        this.setToken(null);
      }
      if (!response.ok) {
        let errorMsg = 'Error en la solicitud';
        try {
          const errData = await response.json();
          errorMsg = errData.message || errData.error || errorMsg;
        } catch (_) {}
        throw new Error(errorMsg);
      }
      return await response.json();
    } catch (err) {
      console.error(`API Error on ${endpoint}:`, err);
      throw err;
    }
  }

  // Auth Endpoints
  async register(data) {
    return this.request('/auth/register', { method: 'POST', body: JSON.stringify(data) });
  }

  async login(data) {
    return this.request('/auth/login', { method: 'POST', body: JSON.stringify(data) });
  }

  async googleLogin(idToken) {
    return this.request('/auth/google', { method: 'POST', body: JSON.stringify({ idToken }) });
  }

  async getProfile() {
    return this.request('/users/me', { method: 'GET' });
  }

  // Route Planning & Navigation Endpoints
  async planRoute(origin, destination, preferences = {}) {
    return this.request('/routes/plan', {
      method: 'POST',
      body: JSON.stringify({
        origin: { latitude: origin.lat, longitude: origin.lng, addressName: origin.name || 'Mi Ubicación' },
        destination: { latitude: destination.lat, longitude: destination.lng, addressName: destination.name || 'Destino' },
        ...preferences
      })
    });
  }

  async getEcoRoute(request) {
    return this.request('/routes/eco-route', { method: 'POST', body: JSON.stringify(request) });
  }

  async recalculateRoute(recalcRequest) {
    return this.request('/routes/recalculate', { method: 'POST', body: JSON.stringify(recalcRequest) });
  }

  async recordTelemetryTick(tickRequest) {
    return this.request('/telemetry/tick', { method: 'POST', body: JSON.stringify(tickRequest) });
  }

  // Trip Recording
  async recordTrip(tripData) {
    return this.request('/trips', { method: 'POST', body: JSON.stringify(tripData) });
  }

  async getTripHistory(page = 0, size = 10) {
    return this.request(`/trips/history?page=${page}&size=${size}`, { method: 'GET' });
  }

  // Dashboard & Leaderboard
  async getDashboardSummary() {
    return this.request('/dashboard/summary', { method: 'GET' });
  }

  async getCommunityImpact() {
    return this.request('/dashboard/community-impact', { method: 'GET' });
  }

  async getLeaderboard() {
    return this.request('/leaderboard', { method: 'GET' });
  }

  // Admin Endpoints
  async getAdminKpis() {
    return this.request('/admin/dashboard/kpis', { method: 'GET' });
  }

  async getAdminUsers(page = 0, size = 20, search = '') {
    const q = search ? `&search=${encodeURIComponent(search)}` : '';
    return this.request(`/admin/users?page=${page}&size=${size}${q}`, { method: 'GET' });
  }

  async updateAdminUserStatus(userId, active) {
    return this.request(`/admin/users/${userId}/status`, { method: 'PATCH', body: JSON.stringify({ active }) });
  }

  async updateAdminUserRole(userId, role) {
    return this.request(`/admin/users/${userId}/role`, { method: 'PATCH', body: JSON.stringify({ role }) });
  }

  async getAdminAuditTrips(page = 0, size = 20, suspiciousOnly = false) {
    return this.request(`/admin/trips/audit?page=${page}&size=${size}&suspiciousOnly=${suspiciousOnly}`, { method: 'GET' });
  }

  async deleteAdminTrip(tripId) {
    return this.request(`/admin/trips/${tripId}`, { method: 'DELETE' });
  }

  async getEmissionFactors() {
    return this.request('/admin/settings/emission-factors', { method: 'GET' });
  }

  async updateEmissionFactor(id, gramsCo2PerKm) {
    return this.request(`/admin/settings/emission-factors/${id}`, { method: 'PUT', body: JSON.stringify({ gramsCo2PerKm }) });
  }
}

window.api = new EcoCommuteApi();
