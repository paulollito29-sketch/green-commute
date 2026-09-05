# 🌿 EcoCommute - Plataforma Web de Movilidad Sostenible & CO₂ (ODS 11)

Plataforma **100% Web (Mobile-First, Zero-Install)** que optimiza rutas de transporte urbano, calcula el ahorro real de huella de carbono ($CO_2$), premia a los usuarios con **EcoPuntos** y medallas, e incluye un **Panel de Administración Integral** para monitoreo y auditoría global.

---

## 🌟 Características Principales

1. **Acceso 100% Web (Sin descargas ni tiendas de apps):**
   - Funciona fluidamente en cualquier navegador móvil (Safari, Chrome) y de escritorio.
   - Navegación responsiva tipo app con barra de pestañas inferior táctil.
2. **GPS y Mapa Interactivo en Vivo:**
   - Mapa OpenStreetMap con Leaflet.js.
   - Geolocalización continua con indicador de precisión y punto azul pulsante.
3. **Optimizador de Rutas en 3 Fases:**
   - **Fase 1 (Normal / Línea Base):** Auto particular solo ($170\text{ g } CO_2/\text{km}$) como comparador base.
   - **Fase 2 (Sostenible / Verde):** Metro, Bus, Bici o Caminata con reducción de emisiones y calorías.
   - **Fase 3 (Sugerencia Smart con IA):** Motor de IA (Google Gemini) que evalúa tráfico, clima, pendientes y ciclovías para recomendar la opción óptima.
4. **Gamificación y Recompensas:**
   - EcoPuntos proporcionales al $CO_2$ evitado.
   - Bonificador de racha diaria ($\text{Streaks } 🔥$).
   - Desbloqueo de insignias automáticas (*Ciclista Urbano, Guardián del Aire, Salvador del Bosque*).
5. **Dashboard de Impacto Ambiental:**
   - Métricas personales: kg de $CO_2$ ahorrados, árboles equivalentes, km limpios y calorías.
   - Gráficos interactivos de tendencias semanales y reparto modal con Chart.js.
6. **Ranking & Impacto Colectivo (ODS 11):**
   - Clasificación de usuarios con medallas de podio (🥇, 🥈, 🥉).
   - Contador de impacto de toda la comunidad (Toneladas de $CO_2$ evitadas).
7. **Panel de Administración (Admin Dashboard):**
   - KPIs globales de la plataforma.
   - Gestión de usuarios (suspensión y cambio de roles).
   - Auditoría de viajes en vivo con detección automática de anomalías y fraudes de velocidad.
   - Calibración en tiempo real de los factores de emisión de $CO_2$ ($g/km$).
8. **Autenticación con Google OAuth2:**
   - Registro e inicio de sesión con 1 clic usando cuenta de Google (Google Identity Services) + autenticación con email/contraseña.

---

## 🛠️ Stack Tecnológico

- **Backend:** Java 21 (LTS), Spring Boot 3.3.3, Spring Security 6 (JWT + Google OAuth2), Spring Data JPA, Virtual Threads (Project Loom).
- **Base de Datos:** H2 (Desarrollo / Pruebas en memoria) & PostgreSQL (Producción en Cloud SQL).
- **Frontend:** HTML5, Tailwind CSS, Leaflet.js, Chart.js, Canvas Confetti.
- **Inteligencia Artificial:** Google Gemini API (`gemini-1.5-flash`).
- **Nube:** Google Cloud Platform (Cloud Run, Container Registry / Artifact Registry).

---

## 🚀 Ejecución Local

### 1. Iniciar el Backend (Java 21)
```bash
cd backend
../apache-maven-3.9.6/bin/mvn spring-boot:run
```

### 2. Abrir en el Navegador
Abre `http://localhost:8080` en tu navegador móvil o de escritorio.

---

## 🔑 Cuentas Demo Preconfiguradas

| Rol | Correo | Contraseña | Características |
| :--- | :--- | :--- | :--- |
| 🛡️ **Administrador** | `admin@ecocommute.org` | `Admin123!` | Acceso completo al panel admin `/admin`, KPIs globales y auditoría. |
| 👤 **Usuario Demo** | `demo@ecocommute.org` | `Demo123!` | Viajes registrados, nivel activo y medallas ganadas. |

*Nota: También puedes usar el botón **"Continuar con Google"** para iniciar sesión instantáneamente con tu cuenta de Google.*

---

## ☁️ Despliegue en Google Cloud Platform (Cloud Run)

Para desplegar en Cloud Run directamente con `gcloud`:

```bash
chmod +x deploy-gcp.sh
./deploy-gcp.sh
```

O manualmente:
```bash
gcloud run deploy ecocommute-web \
  --source . \
  --region us-central1 \
  --allow-unauthenticated \
  --port 8080
```
