// EcoCommute Single Page Application - Auto-Routing, Vehicle Profiles & On-Demand AI
class EcoCommuteApp {
  constructor() {
    this.currentUser = null;
    this.currentRoutes = [];
    this.selectedRoute = null;
    this.selectedProfile = 'BICYCLE'; // 'BICYCLE', 'WALKING', 'DRIVING', 'TRANSIT'
    this.weeklyChart = null;
    this.modalSplitChart = null;
    this.searchDebounceTimer = null;

    this.sampleDestinations = [
      { name: 'Centro Financiero / San Isidro', lat: -12.0965, lng: -77.0285 },
      { name: 'Universidad Central', lat: -12.0820, lng: -77.0490 },
      { name: 'Malecón / Miraflores', lat: -12.1215, lng: -77.0298 },
      { name: 'Estación de Tren Intermodal', lat: -12.0550, lng: -77.0350 }
    ];
  }

  async init() {
    window.mapManager.init('map');

    this.setupNavigation();
    this.setupVehicleModeSelector();
    this.setupAuth();
    this.setupRoutePlanner();
    this.setupAutocomplete();
    this.setupBottomSheet();
    this.setupLiveNavigationHandlers();
    this.setupAdminPanel();

    await this.checkAuthSession();
    await this.loadCommunityImpact();

    // Auto-calculate initial route to sample destination
    setTimeout(() => {
      this.calculateCurrentRoute(false);
    }, 600);
  }

  // VEHICLE / TRANSPORT MODE SELECTOR
  setupVehicleModeSelector() {
    const pills = document.querySelectorAll('.mode-pill');
    pills.forEach(pill => {
      pill.addEventListener('click', () => {
        const mode = pill.getAttribute('data-mode');
        this.selectedProfile = mode;

        pills.forEach(p => {
          p.classList.remove('active', 'text-brand-400', 'bg-slate-850', 'border', 'border-brand-500/40');
          p.classList.add('text-slate-400');
        });

        pill.classList.add('active', 'text-brand-400', 'bg-slate-850', 'border', 'border-brand-500/40');
        pill.classList.remove('text-slate-400');

        // Automatically recalculate route with new vehicle profile (respecting street directions)
        this.calculateCurrentRoute(false);
      });
    });
  }

  // NAVIGATION TABS
  setupNavigation() {
    const navButtons = document.querySelectorAll('.nav-btn');
    navButtons.forEach(btn => {
      btn.addEventListener('click', () => {
        const targetId = btn.getAttribute('data-target');
        this.switchView(targetId);
      });
    });

    document.getElementById('btnRecenterGps')?.addEventListener('click', () => {
      if (window.mapManager.isNavigating) {
        window.mapManager.recenterNavigation();
      } else {
        window.mapManager.recenter();
      }
    });

    document.getElementById('btnGpsLocate')?.addEventListener('click', () => {
      window.mapManager.recenter();
      this.calculateCurrentRoute(false);
    });

    document.getElementById('btnQuickSelectDest')?.addEventListener('click', () => {
      this.promptSampleDestinations();
    });
  }

  setupBottomSheet() {
    const handle = document.getElementById('bottomSheetHandle');
    const sidebar = document.getElementById('mainSidebar');
    if (!handle || !sidebar) return;

    handle.addEventListener('click', () => {
      if (sidebar.classList.contains('sheet-collapsed')) {
        sidebar.classList.remove('sheet-collapsed');
        sidebar.classList.add('sheet-expanded');
      } else {
        sidebar.classList.add('sheet-collapsed');
        sidebar.classList.remove('sheet-expanded');
      }
    });

    const inputDest = document.getElementById('inputDestination');
    inputDest?.addEventListener('focus', () => {
      sidebar.classList.remove('sheet-collapsed');
      sidebar.classList.add('sheet-expanded');
    });

    this.setupDesktopSidebarCollapse(sidebar);
  }

  // Lets the user hide the left panel on desktop to see more of the map,
  // and bring it back with a floating button. Remembers the choice.
  setupDesktopSidebarCollapse(sidebar) {
    const btnCollapse = document.getElementById('btnCollapseSidebarDesktop');
    const btnExpand = document.getElementById('btnExpandSidebarDesktop');
    if (!btnCollapse || !btnExpand) return;

    const setCollapsed = (collapsed) => {
      sidebar.classList.toggle('panel-collapsed', collapsed);
      btnExpand.classList.toggle('hidden', !collapsed);
      btnExpand.classList.toggle('flex', collapsed);
      localStorage.setItem('ecocommute_sidebar_collapsed', collapsed ? '1' : '0');
    };

    btnCollapse.addEventListener('click', () => setCollapsed(true));
    btnExpand.addEventListener('click', () => setCollapsed(false));

    setCollapsed(localStorage.getItem('ecocommute_sidebar_collapsed') === '1');
  }

  switchView(viewId) {
    document.querySelectorAll('.app-view').forEach(view => view.classList.add('hidden'));
    document.querySelectorAll('.nav-btn').forEach(btn => {
      btn.classList.remove('active', 'text-brand-400', 'text-purple-400', 'bg-slate-800/80');
      btn.classList.add('text-slate-400');
    });

    const targetView = document.getElementById(viewId);
    if (targetView) targetView.classList.remove('hidden');

    const activeButtons = document.querySelectorAll(`.nav-btn[data-target="${viewId}"]`);
    activeButtons.forEach(btn => {
      btn.classList.add('active');
      if (viewId === 'viewAdmin') {
        btn.classList.add('text-purple-400', 'bg-purple-950/30');
      } else {
        btn.classList.add('text-brand-400', 'bg-slate-800/80');
      }
    });

    if (viewId === 'viewDashboard') this.loadDashboard();
    if (viewId === 'viewLeaderboard') this.loadLeaderboard();
    if (viewId === 'viewAdmin') this.loadAdminKpis();
    if (viewId === 'viewRoutes' && window.mapManager.map) {
      setTimeout(() => window.mapManager.map.invalidateSize(), 200);
    }
  }

  // SMART DESTINATION SEARCH & AUTOCOMPLETE
  setupAutocomplete() {
    const inputDest = document.getElementById('inputDestination');
    const dropdown = document.getElementById('autocompleteDropdown');
    const btnClear = document.getElementById('btnClearDestination');
    const spinner = document.getElementById('searchSpinner');
    const categoryChips = document.querySelectorAll('.quick-category-chip');

    if (!inputDest || !dropdown) return;

    let highlightedIndex = -1;
    let currentResults = [];

    const renderResults = (results) => {
      currentResults = results;
      highlightedIndex = -1;
      dropdown.innerHTML = '';

      if (!results || results.length === 0) {
        dropdown.classList.add('hidden');
        return;
      }

      results.forEach((item, idx) => {
        const itemEl = document.createElement('div');
        itemEl.className = 'search-item px-3.5 py-2.5 cursor-pointer flex items-center justify-between text-xs transition border-b border-white/5 last:border-0';
        
        const badge = item.isRecent ? '<span class="text-[9px] text-slate-500 font-bold uppercase tracking-wider px-1.5 py-0.5 rounded bg-white/5">Reciente</span>' 
                    : (item.isFeatured ? '<span class="text-[9px] text-brand-400 font-bold uppercase tracking-wider px-1.5 py-0.5 rounded bg-brand-500/10">Destacado</span>' : '');

        const distanceChip = item.distanceKm !== undefined ? `<span class="text-[10px] text-emerald-400 font-bold bg-emerald-500/10 px-2 py-0.5 rounded-full">${item.distanceKm} km</span>` : '';

        itemEl.innerHTML = `
          <div class="flex items-center space-x-2.5 min-w-0 flex-1">
            <span class="text-base shrink-0">${item.icon || '📍'}</span>
            <div class="min-w-0 flex-1">
              <div class="font-bold text-white truncate flex items-center gap-1.5">
                <span>${item.name}</span>
                ${badge}
              </div>
              <div class="text-[10px] text-slate-400 truncate mt-0.5">${item.desc || 'Destino seleccionado'}</div>
            </div>
          </div>
          <div class="shrink-0 ml-2">
            ${distanceChip}
          </div>
        `;

        itemEl.addEventListener('click', () => {
          this.selectSmartDestination(item);
        });

        dropdown.appendChild(itemEl);
      });

      dropdown.classList.remove('hidden');
    };

    const triggerSearch = async (query) => {
      spinner?.classList.remove('hidden');
      btnClear?.classList.toggle('hidden', !query);

      try {
        const userLoc = window.mapManager.currentPosition;
        const results = await window.smartSearchService.search(query, userLoc);
        renderResults(results);
      } catch (err) {
        console.warn('Error en búsqueda inteligente:', err);
      } finally {
        spinner?.classList.add('hidden');
      }
    };

    inputDest.addEventListener('focus', () => {
      triggerSearch(inputDest.value);
    });

    inputDest.addEventListener('input', (e) => {
      const query = e.target.value;
      clearTimeout(this.searchDebounceTimer);
      this.searchDebounceTimer = setTimeout(() => {
        triggerSearch(query);
      }, 250);
    });

    btnClear?.addEventListener('click', () => {
      inputDest.value = '';
      btnClear.classList.add('hidden');
      triggerSearch('');
      inputDest.focus();
    });

    // Keyboard navigation (Arrow keys, Enter, Escape)
    inputDest.addEventListener('keydown', (e) => {
      const items = dropdown.querySelectorAll('.search-item');
      if (dropdown.classList.contains('hidden') || items.length === 0) return;

      if (e.key === 'ArrowDown') {
        e.preventDefault();
        highlightedIndex = (highlightedIndex + 1) % items.length;
        this.updateHighlightedSearchItem(items, highlightedIndex);
      } else if (e.key === 'ArrowUp') {
        e.preventDefault();
        highlightedIndex = (highlightedIndex - 1 + items.length) % items.length;
        this.updateHighlightedSearchItem(items, highlightedIndex);
      } else if (e.key === 'Enter') {
        e.preventDefault();
        if (highlightedIndex >= 0 && currentResults[highlightedIndex]) {
          this.selectSmartDestination(currentResults[highlightedIndex]);
        }
      } else if (e.key === 'Escape') {
        dropdown.classList.add('hidden');
      }
    });

    // Quick Category Filter Chips
    categoryChips.forEach(chip => {
      chip.addEventListener('click', () => {
        const category = chip.getAttribute('data-category');
        inputDest.value = category;
        triggerSearch(category);
      });
    });

    document.addEventListener('click', (e) => {
      if (!inputDest.contains(e.target) && !dropdown.contains(e.target)) {
        dropdown.classList.add('hidden');
      }
    });
  }

  updateHighlightedSearchItem(items, activeIdx) {
    items.forEach((item, idx) => {
      item.classList.toggle('highlighted', idx === activeIdx);
    });
  }

  selectSmartDestination(item) {
    const inputDest = document.getElementById('inputDestination');
    const dropdown = document.getElementById('autocompleteDropdown');
    const btnClear = document.getElementById('btnClearDestination');

    if (inputDest) inputDest.value = item.name;
    if (btnClear) btnClear.classList.remove('hidden');
    if (dropdown) dropdown.classList.add('hidden');

    window.smartSearchService.saveToHistory(item);
    window.mapManager.setDestination(item.lat, item.lng, item.name);

    // Instant route calculation
    this.calculateCurrentRoute(false);
  }

  // AUTHENTICATION & MINIMALIST MODAL
  setupAuth() {
    const authModal = document.getElementById('authModal');
    const btnOpenAuth = document.getElementById('btnOpenAuthModal');
    const btnCloseAuth = document.getElementById('btnCloseAuthModal');
    const btnLogout = document.getElementById('btnLogout');
    const userProfilePill = document.getElementById('userProfilePill');

    let isRegisterMode = false;
    const authTitle = document.getElementById('authModalTitle');
    const regField = document.getElementById('registerNameField');
    const submitBtn = document.getElementById('btnSubmitAuth');
    const togglePrompt = document.getElementById('authTogglePrompt');
    const toggleBtn = document.getElementById('btnToggleAuthMode');

    btnOpenAuth?.addEventListener('click', () => {
      authModal.classList.remove('hidden');
    });

    userProfilePill?.addEventListener('click', () => {
      this.switchView('viewDashboard');
    });

    btnCloseAuth?.addEventListener('click', () => {
      authModal.classList.add('hidden');
    });

    btnLogout?.addEventListener('click', () => {
      if (confirm('¿Deseas cerrar tu sesión?')) {
        this.logout();
      }
    });

    toggleBtn?.addEventListener('click', () => {
      isRegisterMode = !isRegisterMode;
      authTitle.innerText = isRegisterMode ? 'Crear Cuenta EcoCommute' : 'Ingresar a EcoCommute';
      regField.classList.toggle('hidden', !isRegisterMode);
      submitBtn.innerText = isRegisterMode ? 'Registrarme' : 'Iniciar Sesión';
      togglePrompt.innerText = isRegisterMode ? '¿Ya tienes cuenta?' : '¿No tienes cuenta?';
      toggleBtn.innerText = isRegisterMode ? 'Iniciar Sesión' : 'Crear Cuenta';
    });

    document.getElementById('authForm')?.addEventListener('submit', async (e) => {
      e.preventDefault();
      const email = document.getElementById('authEmail').value;
      const password = document.getElementById('authPassword').value;

      try {
        if (isRegisterMode) {
          const fullName = document.getElementById('authName').value || 'Usuario Eco';
          const res = await window.api.register({ fullName, email, password, hasBicycle: true });
          this.handleAuthSuccess(res);
        } else {
          const res = await window.api.login({ email, password });
          this.handleAuthSuccess(res);
        }
      } catch (err) {
        alert(err.message || 'Error en autenticación');
      }
    });

    document.getElementById('btnQuickDemoUser')?.addEventListener('click', () => {
      document.getElementById('authEmail').value = 'demo@ecocommute.org';
      document.getElementById('authPassword').value = 'Demo123!';
      document.getElementById('authForm').requestSubmit();
    });

    document.getElementById('btnQuickDemoAdmin')?.addEventListener('click', () => {
      document.getElementById('authEmail').value = 'admin@ecocommute.org';
      document.getElementById('authPassword').value = 'Admin123!';
      document.getElementById('authForm').requestSubmit();
    });
  }

  async checkAuthSession() {
    if (window.api.token) {
      try {
        const profile = await window.api.getProfile();
        this.updateUserUI(profile);
      } catch (_) {
        this.updateUserUI(null);
        this.openAuthModal();
      }
    } else {
      this.updateUserUI(null);
      this.openAuthModal();
    }
  }

  // Shows the login modal automatically when there's no active session
  // (no saved token, or a saved token that turned out to be invalid/expired).
  // A user who already logged in and kept their session open (valid token
  // in localStorage) skips this and goes straight into the app.
  openAuthModal() {
    document.getElementById('authModal')?.classList.remove('hidden');
  }

  handleAuthSuccess(authResponse) {
    window.api.setToken(authResponse.token);
    this.updateUserUI(authResponse);
    document.getElementById('authModal').classList.add('hidden');
    confetti({ particleCount: 60, spread: 60, origin: { y: 0.8 } });
  }

  updateUserUI(user) {
    this.currentUser = user;
    const btnOpenAuth = document.getElementById('btnOpenAuthModal');
    const userProfilePill = document.getElementById('userProfilePill');
    const userAvatarImg = document.getElementById('userAvatarImg');
    const userFullName = document.getElementById('userFullName');
    const userPointsBadge = document.getElementById('userPointsBadge');
    const btnLogout = document.getElementById('btnLogout');
    const navAdminTab = document.getElementById('navAdminTab');

    if (user) {
      btnOpenAuth?.classList.add('hidden');
      userProfilePill?.classList.remove('hidden');
      btnLogout?.classList.remove('hidden');

      if (userFullName) userFullName.innerText = user.fullName ? user.fullName.split(' ')[0] : 'Usuario';
      if (userPointsBadge) userPointsBadge.innerText = `${user.currentPoints || 0} pts`;
      if (userAvatarImg) userAvatarImg.src = user.avatarUrl || 'https://api.dicebear.com/7.x/bottts/svg?seed=eco';

      if (user.role === 'ROLE_ADMIN') {
        navAdminTab?.classList.remove('hidden');
      } else {
        navAdminTab?.classList.add('hidden');
      }
    } else {
      btnOpenAuth?.classList.remove('hidden');
      userProfilePill?.classList.add('hidden');
      btnLogout?.classList.add('hidden');
      navAdminTab?.classList.add('hidden');
    }
  }

  logout() {
    window.api.setToken(null);
    this.updateUserUI(null);
    this.switchView('viewRoutes');
  }

  promptSampleDestinations() {
    const list = this.sampleDestinations.map((d, i) => `${i + 1}. ${d.name}`).join('\n');
    const choice = prompt(`Selecciona un destino de ejemplo (1-${this.sampleDestinations.length}):\n${list}`, '1');
    const index = parseInt(choice, 10) - 1;
    if (index >= 0 && index < this.sampleDestinations.length) {
      const selected = this.sampleDestinations[index];
      window.mapManager.setDestination(selected.lat, selected.lng, selected.name);
      // Automatically recalculate
      this.calculateCurrentRoute(false);
    }
  }

  // AUTOMATIC ROUTE PLANNING & ON-DEMAND AI
  setupRoutePlanner() {
    // Dedicated ON-DEMAND AI Optimization Button
    document.getElementById('btnOptimizeWithAi')?.addEventListener('click', () => {
      this.calculateCurrentRoute(true);
    });
  }

  async calculateCurrentRoute(enableAi = false) {
    const origin = window.mapManager.currentPosition;
    let dest = window.mapManager.destinationPosition;

    if (!dest) {
      dest = this.sampleDestinations[0];
      window.mapManager.setDestination(dest.lat, dest.lng, dest.name);
    }

    const btnAi = document.getElementById('btnOptimizeWithAi');
    const btnAiText = document.getElementById('btnOptimizeWithAiText');

    if (enableAi && btnAi && btnAiText) {
      btnAiText.innerText = 'Consultando Gemini AI Advisor...';
      btnAi.classList.add('animate-pulse');
    }

    try {
      const response = await window.api.planRoute(
        { lat: origin.lat, lng: origin.lng, name: 'Mi Ubicación' },
        { lat: dest.lat, lng: dest.lng, name: dest.name },
        {
          hasBicycle: this.selectedProfile === 'BICYCLE',
          selectedProfile: this.selectedProfile,
          enableAiOptimization: enableAi
        }
      );

      this.renderRouteComparison(response, enableAi);
    } catch (err) {
      console.error('Error al calcular rutas:', err);
    } finally {
      if (btnAi && btnAiText) {
        btnAiText.innerText = 'Optimizar con IA (Gemini Advisor)';
        btnAi.classList.remove('animate-pulse');
      }
    }
  }

  renderRouteComparison(routeResponse, hasAiInsight = false) {
    this.currentRoutes = [
      routeResponse.aiGreenCorridorRoute,
      routeResponse.standardProfileRoute,
      routeResponse.baselineCarRoute
    ].filter(Boolean);
    const container = document.getElementById('routeCardsContainer');
    container.innerHTML = '';

    const summaryText = document.getElementById('routeSummaryText');
    const baselineCo2 = routeResponse.baselineCarRoute?.co2EmittedGrams ?? 0;
    summaryText.innerText = `Línea Base en Auto: ${(baselineCo2 / 1000).toFixed(2)} kg CO₂`;

    const baseline = routeResponse.baselineCarRoute;
    const sustainable = routeResponse.standardProfileRoute;
    const aiRoute = routeResponse.aiGreenCorridorRoute;

    const cardsToRender = [];

    if (aiRoute) {
      cardsToRender.push({
        route: aiRoute,
        tier: hasAiInsight ? '🌿 RUTA IA ECOLÓGICA (GEMINI OPTIMIZADA)' : '🌿 RUTA IA (CORREDOR VERDE)',
        badgeClass: 'bg-emerald-500/20 text-emerald-300 border-emerald-500/30'
      });
    }

    if (sustainable) {
      cardsToRender.push({
        route: sustainable,
        tier: '⚡ RUTA ESTÁNDAR (MÁS DIRECTA)',
        badgeClass: 'bg-sky-500/20 text-sky-300 border-sky-500/30'
      });
    }

    if (baseline && baseline.mode !== sustainable?.mode) {
      cardsToRender.push({
        route: baseline,
        tier: '🚗 LÍNEA BASE (AUTO PARTICULAR)',
        badgeClass: 'bg-rose-500/20 text-rose-300 border-rose-500/30'
      });
    }

    cardsToRender.forEach((item, index) => {
      const r = item.route;
      const isSelected = index === 0;

      const card = document.createElement('div');
      card.className = `route-card bg-slate-950/70 border border-slate-800 rounded-xl p-3 cursor-pointer ${isSelected ? 'selected' : ''}`;
      card.setAttribute('data-route-id', r.id);

      const aiInsightHtml = r.aiInsight ? `
        <div class="mt-2 pt-2 border-t border-slate-800/80 bg-emerald-950/20 -mx-3 -mb-3 p-2.5 rounded-b-xl">
          <div class="text-[11px] font-bold text-emerald-300 flex items-center gap-1">
            <span>✨</span> Análisis del Corredor Verde
          </div>
          <p class="text-[10px] text-slate-300 mt-0.5 leading-relaxed">${r.aiInsight.ecoReasoning}</p>
          <div class="flex items-center justify-between text-[10px] text-emerald-400 font-semibold mt-1">
            <span>🔥 ${r.aiInsight.healthBenefitSummary}</span>
            <span>🌳 ~${r.aiInsight.treesEquivalentFraction} árboles eq.</span>
          </div>
        </div>
      ` : '';

      card.innerHTML = `
        <div class="flex items-center justify-between">
          <span class="text-[9px] uppercase tracking-wider font-extrabold px-2 py-0.5 rounded-full border ${item.badgeClass}">
            ${item.tier}
          </span>
          <span class="text-xs font-bold ${r.co2SavedGrams > 0 ? 'text-brand-400' : 'text-slate-500'}">
            ${r.co2SavedGrams > 0 ? `+${r.potentialPoints} pts` : '0 pts'}
          </span>
        </div>

        <div class="flex items-center justify-between mt-2">
          <div class="flex items-center space-x-2">
            <span class="text-xl">${this.getModeIcon(r.mode)}</span>
            <div>
              <div class="text-xs font-bold text-white">${r.modeDisplayName}</div>
              <div class="text-[10px] text-slate-400">${r.distanceKm} km • ${r.durationMinutes} min</div>
            </div>
          </div>
          <div class="text-right">
            <div class="text-xs font-black ${r.co2SavedGrams > 0 ? 'text-brand-300' : 'text-rose-400'}">
              ${r.co2SavedGrams > 0 ? `-${(r.co2SavedGrams / 1000).toFixed(2)} kg CO₂` : `${(r.co2EmittedGrams / 1000).toFixed(2)} kg CO₂`}
            </div>
            <div class="text-[9px] text-slate-500">
              ${r.co2SavedGrams > 0 ? 'Ahorro real' : 'Emisión directa'}
            </div>
          </div>
        </div>
        ${aiInsightHtml}
      `;

      card.addEventListener('click', () => {
        this.selectRouteOption(r.id);
      });

      container.appendChild(card);
    });

    const defaultRoute = cardsToRender[0].route;
    this.selectRouteOption(defaultRoute.id);
  }

  selectRouteOption(routeId) {
    const route = this.currentRoutes.find(r => r.id === routeId);
    if (!route) return;

    this.selectedRoute = route;

    document.querySelectorAll('.route-card').forEach(card => {
      card.classList.remove('selected');
      if (card.getAttribute('data-route-id') === routeId) {
        card.classList.add('selected');
      }
    });

    window.mapManager.drawRoutes(this.currentRoutes, routeId);

    const btnStart = document.getElementById('btnStartTripNav');
    const btnText = document.getElementById('btnStartTripNavText');
    if (btnStart && btnText) {
      btnStart.disabled = false;
      btnText.innerText = `Iniciar Trayecto en ${route.modeDisplayName} (+${route.potentialPoints} pts)`;
    }
  }

  // ==========================================
  // REAL-TIME LIVE NAVIGATION (WAZE MODE)
  // ==========================================
  setupLiveNavigationHandlers() {
    const btnStartNav = document.getElementById('btnStartTripNav');
    const btnStopNav = document.getElementById('btnStopNav');
    const btnRecenterNav = document.getElementById('btnRecenterNav');
    const btnToggleVoice = document.getElementById('btnToggleVoice');
    const searchBox = document.getElementById('searchPlannerBox');
    const resultsSec = document.getElementById('resultsComparisonSection');
    const btnAi = document.getElementById('btnOptimizeWithAi');
    const liveHud = document.getElementById('liveNavSidebarHud');
    const topBanner = document.getElementById('topTurnBanner');
    const arrivalModal = document.getElementById('arrivalModal');
    const btnCloseArrival = document.getElementById('btnCloseArrivalModal');

    // Voice toggle
    btnToggleVoice?.addEventListener('click', () => {
      const isEnabled = window.mapManager.toggleVoice();
      btnToggleVoice.innerText = isEnabled ? '🔊' : '🔇';
      btnToggleVoice.title = isEnabled ? 'Voz Activada' : 'Voz Silenciada';
    });

    // Handle GPS lock update
    window.mapManager.onGpsLocked = (pos) => {
      const inputOrigin = document.getElementById('inputOrigin');
      if (inputOrigin) {
        inputOrigin.value = pos.name || `Mi Ubicación (${pos.lat.toFixed(4)}, ${pos.lng.toFixed(4)})`;
      }
      this.calculateCurrentRoute(false);
    };

    const btnSpeed1x = document.getElementById('btnSimSpeed1x');
    const btnSpeed2x = document.getElementById('btnSimSpeed2x');
    const btnSpeed4x = document.getElementById('btnSimSpeed4x');

    const updateSpeedButtons = (activeSpeed) => {
      [btnSpeed1x, btnSpeed2x, btnSpeed4x].forEach(b => {
        if (!b) return;
        b.classList.remove('bg-brand-600', 'text-white');
        b.classList.add('bg-slate-800', 'text-slate-400');
      });
      if (activeSpeed === 1) btnSpeed1x?.classList.add('bg-brand-600', 'text-white');
      if (activeSpeed === 2) btnSpeed2x?.classList.add('bg-brand-600', 'text-white');
      if (activeSpeed === 4) btnSpeed4x?.classList.add('bg-brand-600', 'text-white');
    };

    btnSpeed1x?.addEventListener('click', () => { window.mapManager.setSimulationSpeed(1); updateSpeedButtons(1); });
    btnSpeed2x?.addEventListener('click', () => { window.mapManager.setSimulationSpeed(2); updateSpeedButtons(2); });
    btnSpeed4x?.addEventListener('click', () => { window.mapManager.setSimulationSpeed(4); updateSpeedButtons(4); });

    // START TRIP
    btnStartNav?.addEventListener('click', async () => {
      if (!this.selectedRoute) {
        if (!window.mapManager.destinationPosition) {
          const defaultDest = this.sampleDestinations[0];
          window.mapManager.setDestination(defaultDest.lat, defaultDest.lng, defaultDest.name);
        }
        await this.calculateCurrentRoute(false);
      }

      if (!this.selectedRoute) return;

      searchBox?.classList.add('hidden');
      resultsSec?.classList.add('hidden');
      btnAi?.classList.add('hidden');
      btnStartNav?.classList.add('hidden');

      liveHud?.classList.remove('hidden');
      topBanner?.classList.remove('hidden');

      document.getElementById('turnInstructionMode').innerText = this.selectedRoute.modeDisplayName;

      // Inicia navegación activa de inmediato
      window.mapManager.startLiveNavigation(this.selectedRoute, true, 1);
      updateSpeedButtons(1);

      if (btnPlayPauseIcon && btnPlayPauseText) {
        btnPlayPauseIcon.innerText = '⏸️';
        btnPlayPauseText.innerText = 'Pausar Avance';
      }
    });

    // Play / Pause Advance Toggle
    const btnPlayPause = document.getElementById('btnPlayPauseNav');
    const btnPlayPauseIcon = document.getElementById('btnPlayPauseIcon');
    const btnPlayPauseText = document.getElementById('btnPlayPauseText');

    btnPlayPause?.addEventListener('click', () => {
      const isPlaying = window.mapManager.togglePlayPause();
      if (btnPlayPauseIcon && btnPlayPauseText) {
        btnPlayPauseIcon.innerText = isPlaying ? '⏸️' : '▶️';
        btnPlayPauseText.innerText = isPlaying ? 'Pausar Avance' : 'Iniciar Avance';
      }
    });

    btnRecenterNav?.addEventListener('click', () => {
      window.mapManager.recenterNavigation();
    });

    btnStopNav?.addEventListener('click', () => {
      if (confirm('¿Deseas cancelar el trayecto en curso?')) {
        this.exitLiveNavigationUI();
      }
    });

    window.mapManager.onNavUpdate = (data) => {
      document.getElementById('navLiveProgressBar').style.width = `${data.progressPercent}%`;
      document.getElementById('navLiveSpeed').innerHTML = `${data.currentSpeed} <span class="text-[9px] text-slate-400 font-normal">km/h</span>`;
      document.getElementById('navLiveRemaining').innerHTML = `${data.remainingDistanceKm} <span class="text-[9px] font-normal">km</span>`;
      document.getElementById('navLiveCo2').innerHTML = `${data.currentCo2SavedGrams} <span class="text-[9px] font-normal">g</span>`;

      document.getElementById('turnInstructionIcon').innerText = data.nextInstruction.icon;
      document.getElementById('turnInstructionText').innerText = data.nextInstruction.text;
      document.getElementById('turnInstructionEta').innerText = `${data.remainingMinutes} min restantes (${data.progressPercent}%)`;

      if (btnPlayPauseIcon && btnPlayPauseText) {
        btnPlayPauseIcon.innerText = data.isPlaying ? '⏸️' : '▶️';
        btnPlayPauseText.innerText = data.isPlaying ? 'Pausar Avance' : 'Iniciar Avance';
      }
    };

    // Dynamic 25m Off-Route Recalculation Handler
    let isRecalculating = false;
    window.mapManager.onOffRouteDetected = async (point) => {
      if (isRecalculating || !this.selectedRoute) return;
      isRecalculating = true;

      const topInstruction = document.getElementById('turnInstructionText');
      if (topInstruction) topInstruction.innerText = '⚠️ Desvío detectado: recalculando ruta...';

      try {
        const dest = window.mapManager.destinationPosition || this.sampleDestinations[0];
        const res = await window.api.recalculateRoute({
          tripId: this.selectedRoute.tripId || '00000000-0000-0000-0000-000000000000',
          currentLat: point.latitude,
          currentLng: point.longitude,
          destinationLat: dest.lat,
          destinationLng: dest.lng,
          vehicleMode: this.selectedProfile,
          accumulatedCo2SavedGrams: window.mapManager.accumulatedCo2SavedGrams,
          accumulatedDistanceKm: 0.0
        });

        if (res && res.pathCoordinates) {
          const updatedRoute = {
            ...this.selectedRoute,
            distanceKm: res.totalDistanceKm,
            durationMinutes: Math.ceil(res.totalDurationSeconds / 60),
            pathCoordinates: res.pathCoordinates,
            co2SavedGrams: res.estimatedCo2SavedGrams
          };
          this.selectedRoute = updatedRoute;
          window.mapManager.updateRouteOnRecalculate(updatedRoute);
        }
      } catch (err) {
        console.warn('Error en recálculo dinámico:', err);
      } finally {
        setTimeout(() => { isRecalculating = false; }, 3000);
      }
    };

    window.mapManager.onNavFinished = async (route) => {
      this.exitLiveNavigationUI();

      confetti({ particleCount: 150, spread: 90, origin: { y: 0.6 } });

      let pointsEarned = route.potentialPoints;
      let co2SavedGrams = route.co2SavedGrams;
      let caloriesBurned = route.caloriesBurned;

      if (this.currentUser) {
        try {
          const dest = window.mapManager.destinationPosition || this.sampleDestinations[0];
          const res = await window.api.recordTrip({
            transportMode: route.mode,
            originName: 'Mi Ubicación',
            originLat: window.mapManager.currentPosition.lat,
            originLng: window.mapManager.currentPosition.lng,
            destinationName: dest.name,
            destinationLat: dest.lat,
            destinationLng: dest.lng,
            distanceKm: route.distanceKm,
            durationMinutes: route.durationMinutes
          });

          pointsEarned = res.pointsEarned;
          co2SavedGrams = res.co2SavedGrams;
          caloriesBurned = res.caloriesBurned;
          await this.checkAuthSession();
        } catch (err) {
          console.warn('Auto-record trip error:', err);
        }
      }

      document.getElementById('arrivalCo2Saved').innerText = `+${(co2SavedGrams / 1000).toFixed(2)} kg`;
      document.getElementById('arrivalPoints').innerText = `+${pointsEarned} EcoPuntos`;
      document.getElementById('arrivalCalories').innerText = `${caloriesBurned} kcal`;
      arrivalModal?.classList.remove('hidden');
    };

    btnCloseArrival?.addEventListener('click', () => {
      arrivalModal?.classList.add('hidden');
      if (this.currentUser) {
        this.switchView('viewDashboard');
      }
    });
  }

  exitLiveNavigationUI() {
    window.mapManager.stopLiveNavigation();

    document.getElementById('searchPlannerBox')?.classList.remove('hidden');
    document.getElementById('resultsComparisonSection')?.classList.remove('hidden');
    document.getElementById('btnOptimizeWithAi')?.classList.remove('hidden');
    document.getElementById('btnStartTripNav')?.classList.remove('hidden');

    document.getElementById('liveNavSidebarHud')?.classList.add('hidden');
    document.getElementById('topTurnBanner')?.classList.add('hidden');
    document.getElementById('btnRecenterNav')?.classList.add('hidden');

    window.mapManager.drawRoutes(this.currentRoutes, this.selectedRoute?.id);
  }

  getModeIcon(mode) {
    switch (mode) {
      case 'CAR_SOLO': return '🚗';
      case 'BICYCLE': return '🚲';
      case 'WALKING': return '🚶';
      default: return '📍';
    }
  }

  // DASHBOARD
  async loadDashboard() {
    if (!this.currentUser) return;

    try {
      const data = await window.api.getDashboardSummary();

      document.getElementById('dashCo2Saved').innerText = data.totalCo2SavedKg.toFixed(1);
      document.getElementById('dashTreesEq').innerText = data.treesEquivalent.toFixed(2);
      document.getElementById('dashDistance').innerText = data.totalDistanceKm.toFixed(1);
      document.getElementById('dashCalories').innerText = data.totalCaloriesBurned;

      const levelNames = ['', 'Semilla Verde 🌱', 'Brote Urbano 🌿', 'Árbol Sostenible 🌳', 'Guardián del Aire 🛡️', 'Héroe Climático ⚡', 'Maestro del Ecosistema 👑'];
      document.getElementById('dashUserLevelBadge').innerText = `Nivel ${data.currentLevel}: ${levelNames[data.currentLevel] || 'Eco Expert'}`;

      this.renderWeeklyChart(data.weeklyTrend);
      this.renderModalSplitChart(data.tripsByMode);
      this.renderBadges(data.recentBadges);
    } catch (err) {
      console.error('Dashboard load error:', err);
    }
  }

  renderWeeklyChart(weeklyTrend) {
    const ctx = document.getElementById('weeklyChart').getContext('2d');
    if (this.weeklyChart) this.weeklyChart.destroy();

    const labels = weeklyTrend.map(d => d.dayOfWeek);
    const co2Data = weeklyTrend.map(d => d.co2SavedGrams);

    this.weeklyChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: labels,
        datasets: [{
          label: 'CO₂ Ahorrado (g)',
          data: co2Data,
          backgroundColor: 'rgba(16, 185, 129, 0.7)',
          borderColor: '#10b981',
          borderWidth: 1.5,
          borderRadius: 6
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          x: { grid: { display: false }, ticks: { color: '#64748b', font: { size: 10 } } },
          y: { grid: { color: 'rgba(51, 65, 85, 0.3)' }, ticks: { color: '#64748b', font: { size: 10 } } }
        }
      }
    });
  }

  renderModalSplitChart(tripsByMode) {
    const ctx = document.getElementById('modalSplitChart').getContext('2d');
    if (this.modalSplitChart) this.modalSplitChart.destroy();

    const labels = Object.keys(tripsByMode);
    const counts = Object.values(tripsByMode);

    if (labels.length === 0) {
      labels.push('Sin viajes aún');
      counts.push(1);
    }

    this.modalSplitChart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: labels,
        datasets: [{
          data: counts,
          backgroundColor: ['#10b981', '#3b82f6', '#8b5cf6', '#f59e0b', '#f43f5e', '#06b6d4'],
          borderWidth: 0
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'bottom', labels: { color: '#94a3b8', font: { size: 9 }, boxWidth: 8 } }
        }
      }
    });
  }

  renderBadges(badges) {
    const container = document.getElementById('badgesContainer');
    container.innerHTML = '';

    badges.forEach(b => {
      const el = document.createElement('div');
      el.className = `p-3 rounded-xl border flex items-center space-x-2.5 ${b.unlocked ? 'bg-slate-950/70 border-brand-500/40' : 'bg-slate-950/30 border-slate-800/60 opacity-50'}`;
      el.innerHTML = `
        <span class="text-2xl">${b.iconEmoji}</span>
        <div class="flex-1 min-w-0">
          <div class="text-[11px] font-bold text-white truncate flex items-center justify-between">
            <span>${b.title}</span>
            ${b.unlocked ? '<span class="text-brand-400 text-[10px]">✓</span>' : `<span class="text-slate-500 text-[9px]">${b.progressPercent}%</span>`}
          </div>
          <p class="text-[9px] text-slate-400 truncate">${b.description}</p>
        </div>
      `;
      container.appendChild(el);
    });
  }

  // LEADERBOARD & COMMUNITY IMPACT
  async loadCommunityImpact() {
    try {
      const impact = await window.api.getCommunityImpact();
      document.getElementById('commTotalCo2').innerText = `${impact.totalCo2SavedTons.toFixed(2)} t`;
      document.getElementById('commTotalTrees').innerText = Math.round(impact.treesPlantedEquivalent);
      document.getElementById('commTotalKm').innerText = Math.round(impact.totalDistanceKm);
    } catch (_) {}
  }

  async loadLeaderboard() {
    await this.loadCommunityImpact();
    const list = document.getElementById('leaderboardList');
    list.innerHTML = '<div class="text-center py-4 text-xs text-slate-400">Cargando clasificación...</div>';

    try {
      const entries = await window.api.getLeaderboard();
      list.innerHTML = '';

      entries.forEach(u => {
        const isMe = this.currentUser && this.currentUser.id === u.userId;
        let rankBadge = `<span class="w-6 h-6 rounded-full bg-slate-800 text-slate-400 text-xs font-bold flex items-center justify-center">${u.rank}</span>`;
        if (u.rank === 1) rankBadge = `<span class="w-6 h-6 rounded-full bg-amber-400 text-slate-950 text-xs font-black flex items-center justify-center shadow-md shadow-amber-400/40">🥇</span>`;
        if (u.rank === 2) rankBadge = `<span class="w-6 h-6 rounded-full bg-slate-300 text-slate-950 text-xs font-black flex items-center justify-center">🥈</span>`;
        if (u.rank === 3) rankBadge = `<span class="w-6 h-6 rounded-full bg-amber-700 text-white text-xs font-black flex items-center justify-center">🥉</span>`;

        const row = document.createElement('div');
        row.className = `flex items-center justify-between p-3 rounded-xl border transition ${isMe ? 'bg-brand-950/30 border-brand-500/50' : 'bg-slate-950/60 border-slate-800/80'}`;

        row.innerHTML = `
          <div class="flex items-center space-x-3">
            ${rankBadge}
            <img src="${u.avatarUrl || 'https://api.dicebear.com/7.x/bottts/svg?seed=' + u.userId}" class="w-8 h-8 rounded-full bg-slate-800 border border-slate-700" />
            <div>
              <div class="text-xs font-bold text-white flex items-center gap-1.5">
                <span>${u.fullName}</span>
                ${isMe ? '<span class="text-[9px] px-1 py-0.2 rounded bg-brand-500/20 text-brand-400 font-extrabold">TÚ</span>' : ''}
              </div>
              <div class="text-[10px] text-slate-400">${u.tripsCount} viajes • 🔥 ${u.streakDays} días</div>
            </div>
          </div>
          <div class="text-right">
            <div class="text-xs font-black text-brand-300">${u.currentPoints} pts</div>
            <div class="text-[10px] text-emerald-400 font-semibold">${u.totalCo2SavedKg.toFixed(2)} kg CO₂</div>
          </div>
        `;
        list.appendChild(row);
      });
    } catch (err) {
      list.innerHTML = `<div class="text-center py-4 text-xs text-rose-400">Error: ${err.message}</div>`;
    }
  }

  // ADMIN PANEL
  setupAdminPanel() {
    const tabs = [
      { btn: 'adminTabKpis', content: 'adminContentKpis' },
      { btn: 'adminTabUsers', content: 'adminContentUsers', load: () => this.loadAdminUsers() },
      { btn: 'adminTabAudit', content: 'adminContentAudit', load: () => this.loadAdminAudit() },
      { btn: 'adminTabSettings', content: 'adminContentSettings', load: () => this.loadAdminEmissionFactors() }
    ];

    tabs.forEach(t => {
      document.getElementById(t.btn)?.addEventListener('click', () => {
        tabs.forEach(other => {
          document.getElementById(other.btn).classList.remove('bg-purple-600', 'text-white');
          document.getElementById(other.btn).classList.add('text-slate-400');
          document.getElementById(other.content).classList.add('hidden');
        });
        document.getElementById(t.btn).classList.add('bg-purple-600', 'text-white');
        document.getElementById(t.content).classList.remove('hidden');
        if (t.load) t.load();
      });
    });

    document.getElementById('btnRefreshUsers')?.addEventListener('click', () => this.loadAdminUsers());
    document.getElementById('adminUserSearch')?.addEventListener('input', (e) => this.loadAdminUsers(e.target.value));
    document.getElementById('btnRefreshAudit')?.addEventListener('click', () => this.loadAdminAudit());
    document.getElementById('checkSuspiciousOnly')?.addEventListener('change', () => this.loadAdminAudit());
  }

  async loadAdminKpis() {
    try {
      const kpis = await window.api.getAdminKpis();
      const grid = document.getElementById('adminKpiGrid');
      grid.innerHTML = `
        <div class="bg-slate-900 p-4 rounded-2xl border border-slate-800">
          <div class="text-xs text-purple-400 font-semibold">Total Usuarios</div>
          <div class="text-2xl font-black text-white mt-1">${kpis.totalRegisteredUsers}</div>
          <div class="text-[10px] text-slate-400">${kpis.activeUsersCount} activos</div>
        </div>
        <div class="bg-slate-900 p-4 rounded-2xl border border-slate-800">
          <div class="text-xs text-brand-400 font-semibold">Total Viajes</div>
          <div class="text-2xl font-black text-white mt-1">${kpis.totalTripsLogged}</div>
          <div class="text-[10px] text-slate-400">${kpis.averageCo2SavedPerTripGrams}g CO₂ prom./viaje</div>
        </div>
        <div class="bg-slate-900 p-4 rounded-2xl border border-slate-800">
          <div class="text-xs text-emerald-400 font-semibold">CO₂ Total Evitado</div>
          <div class="text-2xl font-black text-white mt-1">${(kpis.totalCo2SavedKg / 1000).toFixed(2)} <span class="text-xs">Ton</span></div>
          <div class="text-[10px] text-slate-400">~${kpis.totalTreesEquivalent} árboles</div>
        </div>
        <div class="bg-slate-900 p-4 rounded-2xl border border-slate-800">
          <div class="text-xs text-rose-400 font-semibold">Alertas de Fraude</div>
          <div class="text-2xl font-black text-white mt-1">${kpis.suspiciousTripsCount}</div>
          <div class="text-[10px] text-slate-400">Viajes con anomalías</div>
        </div>
      `;
    } catch (_) {}
  }

  async loadAdminUsers(search = '') {
    const tbody = document.getElementById('adminUsersTableBody');
    tbody.innerHTML = '<tr><td colspan="6" class="p-3 text-center text-slate-400">Cargando usuarios...</td></tr>';

    try {
      const page = await window.api.getAdminUsers(0, 20, search);
      tbody.innerHTML = '';

      page.content.forEach(u => {
        const tr = document.createElement('tr');
        tr.className = 'hover:bg-slate-850';
        tr.innerHTML = `
          <td class="p-3 font-medium text-white flex items-center space-x-2">
            <img src="${u.avatarUrl || 'https://api.dicebear.com/7.x/bottts/svg?seed=' + u.id}" class="w-6 h-6 rounded-full" />
            <span>${u.fullName}</span>
          </td>
          <td class="p-3 text-slate-400">${u.email}</td>
          <td class="p-3">
            <select class="role-select bg-slate-950 border border-slate-700 rounded px-1.5 py-0.5 text-xs text-purple-300" data-user-id="${u.id}">
              <option value="ROLE_USER" ${u.role === 'ROLE_USER' ? 'selected' : ''}>USER</option>
              <option value="ROLE_ADMIN" ${u.role === 'ROLE_ADMIN' ? 'selected' : ''}>ADMIN</option>
            </select>
          </td>
          <td class="p-3 text-brand-300 font-bold">${u.currentPoints} pts / ${u.totalCo2SavedKg}kg</td>
          <td class="p-3">
            <span class="px-2 py-0.5 rounded text-[10px] font-bold ${u.active ? 'bg-emerald-500/20 text-emerald-400' : 'bg-rose-500/20 text-rose-400'}">
              ${u.active ? 'Activo' : 'Suspendido'}
            </span>
          </td>
          <td class="p-3 text-right">
            <button class="btn-toggle-status text-[11px] px-2.5 py-1 rounded ${u.active ? 'bg-rose-600/20 text-rose-300 hover:bg-rose-600/40' : 'bg-emerald-600/20 text-emerald-300 hover:bg-emerald-600/40'}" data-user-id="${u.id}" data-current-active="${u.active}">
              ${u.active ? 'Suspender' : 'Reactivar'}
            </button>
          </td>
        `;
        tbody.appendChild(tr);
      });

      document.querySelectorAll('.btn-toggle-status').forEach(btn => {
        btn.addEventListener('click', async (e) => {
          const uid = e.target.getAttribute('data-user-id');
          const current = e.target.getAttribute('data-current-active') === 'true';
          await window.api.updateAdminUserStatus(uid, !current);
          this.loadAdminUsers();
        });
      });

      document.querySelectorAll('.role-select').forEach(sel => {
        sel.addEventListener('change', async (e) => {
          const uid = e.target.getAttribute('data-user-id');
          const newRole = e.target.value;
          await window.api.updateAdminUserRole(uid, newRole);
          alert(`Rol actualizado a ${newRole}`);
        });
      });
    } catch (err) {
      tbody.innerHTML = `<tr><td colspan="6" class="p-3 text-center text-rose-400">Error: ${err.message}</td></tr>`;
    }
  }

  async loadAdminAudit() {
    const suspiciousOnly = document.getElementById('checkSuspiciousOnly').checked;
    const tbody = document.getElementById('adminAuditTableBody');
    tbody.innerHTML = '<tr><td colspan="7" class="p-3 text-center text-slate-400">Cargando auditoría...</td></tr>';

    try {
      const page = await window.api.getAdminAuditTrips(0, 20, suspiciousOnly);
      tbody.innerHTML = '';

      if (page.content.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="p-4 text-center text-slate-400">No hay viajes registrados bajo este criterio.</td></tr>';
        return;
      }

      page.content.forEach(t => {
        const tr = document.createElement('tr');
        tr.className = 'hover:bg-slate-850';
        tr.innerHTML = `
          <td class="p-3 text-white font-medium">${t.userFullName}</td>
          <td class="p-3 text-slate-300">${t.transportMode}</td>
          <td class="p-3 text-slate-400">${t.distanceKm} km (${t.durationMinutes} min)</td>
          <td class="p-3 font-bold ${t.speedKmh > 30 ? 'text-amber-400' : 'text-slate-300'}">${t.speedKmh} km/h</td>
          <td class="p-3 text-brand-400 font-bold">${(t.co2SavedGrams / 1000).toFixed(2)} kg</td>
          <td class="p-3">
            ${t.suspicious ? `<span class="text-[10px] px-2 py-0.5 rounded bg-rose-500/20 text-rose-300 font-bold" title="${t.suspiciousReason}">⚠️ Anomalía</span>` : '<span class="text-[10px] text-emerald-400">✓ Válido</span>'}
          </td>
          <td class="p-3 text-right">
            <button class="btn-delete-trip text-[11px] px-2.5 py-1 rounded bg-rose-600/20 text-rose-300 hover:bg-rose-600/40" data-trip-id="${t.id}">
              Anular
            </button>
          </td>
        `;
        tbody.appendChild(tr);
      });

      document.querySelectorAll('.btn-delete-trip').forEach(btn => {
        btn.addEventListener('click', async (e) => {
          const tid = e.target.getAttribute('data-trip-id');
          if (confirm('¿Anular este viaje y revertir los puntos/CO2 del usuario?')) {
            await window.api.deleteAdminTrip(tid);
            this.loadAdminAudit();
            this.loadAdminKpis();
          }
        });
      });
    } catch (err) {
      tbody.innerHTML = `<tr><td colspan="7" class="p-3 text-center text-rose-400">Error: ${err.message}</td></tr>`;
    }
  }

  async loadAdminEmissionFactors() {
    const grid = document.getElementById('adminEmissionFactorsGrid');
    grid.innerHTML = '<div class="text-xs text-slate-400">Cargando factores...</div>';

    try {
      const factors = await window.api.getEmissionFactors();
      grid.innerHTML = '';

      factors.forEach(f => {
        const card = document.createElement('div');
        card.className = 'bg-slate-900 p-4 rounded-2xl border border-slate-800 flex items-center justify-between space-x-3';
        card.innerHTML = `
          <div>
            <div class="text-xs font-bold text-white">${f.modeName}</div>
            <div class="text-[10px] text-slate-400">${f.description}</div>
          </div>
          <div class="flex items-center space-x-2">
            <input type="number" step="1.0" class="input-factor bg-slate-950 text-xs text-brand-300 font-bold w-16 px-2 py-1.5 rounded-lg border border-slate-700 text-right focus:outline-none focus:border-brand-500" value="${f.gramsCo2PerKm}" data-factor-id="${f.id}" />
            <span class="text-[10px] text-slate-400">g/km</span>
            <button class="btn-save-factor text-[11px] bg-brand-600 hover:bg-brand-500 text-white font-bold px-2.5 py-1.5 rounded-lg transition" data-factor-id="${f.id}">
              Guardar
            </button>
          </div>
        `;
        grid.appendChild(card);
      });

      document.querySelectorAll('.btn-save-factor').forEach(btn => {
        btn.addEventListener('click', async (e) => {
          const fid = e.target.getAttribute('data-factor-id');
          const input = document.querySelector(`.input-factor[data-factor-id="${fid}"]`);
          const val = parseFloat(input.value);
          if (!isNaN(val)) {
            await window.api.updateEmissionFactor(fid, val);
            alert('Factor de emisión actualizado correctamente');
          }
        });
      });
    } catch (err) {
      grid.innerHTML = `<div class="text-xs text-rose-400">Error: ${err.message}</div>`;
    }
  }
}

document.addEventListener('DOMContentLoaded', () => {
  window.app = new EcoCommuteApp();
  window.app.init();
});
