-- =========================================================================
-- EcoCommute - Esquema PostgreSQL + PostGIS de Navegación Activa y Telemetría
-- =========================================================================

CREATE EXTENSION IF NOT EXISTS postgis;

-- Tabla de Viajes Activos y Rutas Planificadas
CREATE TABLE IF NOT EXISTS trips (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    transport_mode VARCHAR(30) NOT NULL, -- 'BICYCLE', 'WALKING', 'DRIVING'
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS', -- 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'
    
    -- Geometrías espaciales en WGS 84 (SRID 4326)
    origin_geom GEOMETRY(Point, 4326) NOT NULL,
    destination_geom GEOMETRY(Point, 4326) NOT NULL,
    route_geom GEOMETRY(LineString, 4326) NOT NULL,
    
    -- Métricas planificadas vs reales
    planned_distance_meters DOUBLE PRECISION NOT NULL,
    planned_duration_seconds INTEGER NOT NULL,
    actual_distance_meters DOUBLE PRECISION DEFAULT 0.0,
    actual_duration_seconds INTEGER DEFAULT 0,
    
    -- Telemetría de Carbono (Gramos)
    baseline_co2_grams DOUBLE PRECISION NOT NULL,
    actual_co2_grams DOUBLE PRECISION DEFAULT 0.0,
    accumulated_co2_saved_grams DOUBLE PRECISION DEFAULT 0.0,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Índices Espaciales GiST
CREATE INDEX IF NOT EXISTS idx_trips_route_geom ON trips USING GIST (route_geom);
CREATE INDEX IF NOT EXISTS idx_trips_origin_geom ON trips USING GIST (origin_geom);
CREATE INDEX IF NOT EXISTS idx_trips_destination_geom ON trips USING GIST (destination_geom);

-- Tabla de Puntos de Telemetría Continua
CREATE TABLE IF NOT EXISTS trip_telemetry (
    id BIGSERIAL PRIMARY KEY,
    trip_id UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    location GEOMETRY(Point, 4326) NOT NULL,
    altitude DOUBLE PRECISION,
    speed_kmh DOUBLE PRECISION NOT NULL,
    bearing DOUBLE PRECISION,
    distance_from_last_tick_meters DOUBLE PRECISION NOT NULL,
    
    -- Emisiones registradas en el tick
    tick_co2_emitted_grams DOUBLE PRECISION NOT NULL,
    tick_co2_saved_grams DOUBLE PRECISION NOT NULL,
    
    -- Distancia perpendicular a la ruta activa (en metros)
    off_route_distance_meters DOUBLE PRECISION,
    is_off_route BOOLEAN DEFAULT FALSE,
    
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_telemetry_trip_id ON trip_telemetry (trip_id);
CREATE INDEX IF NOT EXISTS idx_telemetry_location ON trip_telemetry USING GIST (location);
