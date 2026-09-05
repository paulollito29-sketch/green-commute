package com.ecocommute.config;

import com.ecocommute.entity.*;
import com.ecocommute.dto.trip.TripCreateRequest;
import com.ecocommute.repository.*;
import com.ecocommute.service.GamificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final UserStatsRepository userStatsRepository;
    private final BadgeRepository badgeRepository;
    private final EmissionFactorRepository emissionFactorRepository;
    private final PasswordEncoder passwordEncoder;
    private final GamificationService gamificationService;

    public DataInitializer(UserRepository userRepository,
                           UserStatsRepository userStatsRepository,
                           BadgeRepository badgeRepository,
                           EmissionFactorRepository emissionFactorRepository,
                           PasswordEncoder passwordEncoder,
                           GamificationService gamificationService) {
        this.userRepository = userRepository;
        this.userStatsRepository = userStatsRepository;
        this.badgeRepository = badgeRepository;
        this.emissionFactorRepository = emissionFactorRepository;
        this.passwordEncoder = passwordEncoder;
        this.gamificationService = gamificationService;
    }

    @Override
    public void run(String... args) {
        initEmissionFactors();
        initBadges();
        initUsersAndTrips();
        log.info("EcoCommute Database populated with rich demo and community data!");
    }

    private void initEmissionFactors() {
        if (emissionFactorRepository.count() == 0) {
            for (TransportMode mode : TransportMode.values()) {
                emissionFactorRepository.save(new EmissionFactor(
                        mode,
                        mode.getCo2GramsPerKm(),
                        "Factor estándar DEFRA / GHG Protocol para " + mode.getDisplayName()
                ));
            }
        }
    }

    private void initBadges() {
        if (badgeRepository.count() == 0) {
            badgeRepository.saveAll(List.of(
                    new Badge("FIRST_STEP", "Primer Paso Verde", "Completaste tu primer viaje sostenible en EcoCommute", "🌱", 10, 0.0, 0, 1),
                    new Badge("BIKE_CHAMP", "Ciclista Urbano", "Ahorraste tus primeros 5 kg de CO₂ pedaleando", "🚲", 50, 5.0, 0, 3),
                    new Badge("TRANSIT_PRO", "Guardián del Aire", "Completaste 10 viajes en transporte público o metro", "🚇", 150, 15.0, 0, 10),
                    new Badge("STREAK_7", "Semana Imparable", "Mantuviste una racha de 7 días consecutivos de transporte limpio", "🔥", 300, 20.0, 7, 7),
                    new Badge("FOREST_HERO", "Salvador del Bosque", "Evitaste más de 50 kg de CO₂ (equivalente a más de 2 árboles maduros)", "🌳", 600, 50.0, 0, 20),
                    new Badge("ZERO_EMISSION", "Maestro Cero Emisiones", "Acumulaste 100 km recorridos a pie o en bicicleta mecánica", "⚡", 1000, 100.0, 14, 30)
            ));
        }
    }

    private void initUsersAndTrips() {
        if (userRepository.count() == 0) {
            // 1. Admin User
            User admin = createUser("admin@ecocommute.org", "Admin123!", "Administrador EcoCommute", Role.ROLE_ADMIN, "admin", true);

            // 2. Demo User (Elena Rios) - High engagement
            User elena = createUser("demo@ecocommute.org", "Demo123!", "Elena Rios", Role.ROLE_USER, "elena", true);

            // 3. Mateo Silva - Leaderboard #1 Ciclista Top
            User mateo = createUser("mateo@ecocommute.org", "Mateo123!", "Mateo Silva", Role.ROLE_USER, "mateo", true);

            // 4. Sofia Morales - Metro & Transit Expert
            User sofia = createUser("sofia@ecocommute.org", "Sofia123!", "Sofia Morales", Role.ROLE_USER, "sofia", false);

            // 5. Carlos Mendoza - Multimodal
            User carlos = createUser("carlos@ecocommute.org", "Carlos123!", "Carlos Mendoza", Role.ROLE_USER, "carlos", true);

            // 6. Lucía Vega - E-Bike commuter
            User lucia = createUser("lucia@ecocommute.org", "Lucia123!", "Lucía Vega", Role.ROLE_USER, "lucia", true);

            // 7. Diego Torres - Suspicious User (for Admin Auditor demo)
            User diego = createUser("diego@ecocommute.org", "Diego123!", "Diego Torres", Role.ROLE_USER, "diego", false);

            // Seed Elena Trips (Daily variety for beautiful charts)
            gamificationService.recordTrip(elena.getId(), new TripCreateRequest(
                    TransportMode.BICYCLE, "Casa (San Isidro)", -12.0897, -77.0543, "Centro Financiero", -12.0965, -77.0285, 8.5, 32
            ));
            gamificationService.recordTrip(elena.getId(), new TripCreateRequest(
                    TransportMode.BICYCLE, "Parque Kennedy", -12.1215, -77.0298, "Café Verde Miraflores", -12.1280, -77.0310, 1.6, 6
            ));
            gamificationService.recordTrip(elena.getId(), new TripCreateRequest(
                    TransportMode.WALKING, "Parque Kennedy", -12.1215, -77.0298, "Café Verde Miraflores", -12.1280, -77.0310, 1.6, 18
            ));
            gamificationService.recordTrip(elena.getId(), new TripCreateRequest(
                    TransportMode.BICYCLE, "Av. Salaverry", -12.0850, -77.0450, "Malecón de la Reserva", -12.1310, -77.0290, 6.8, 20
            ));

            // Seed Mateo Trips (High mileage cyclist)
            for (int i = 0; i < 6; i++) {
                gamificationService.recordTrip(mateo.getId(), new TripCreateRequest(
                        TransportMode.BICYCLE, "Residencial San Felipe", -12.0820, -77.0490, "Parque de la Exposición", -12.0590, -77.0360, 12.0, 42
                ));
            }

            // Seed Sofia Trips
            for (int i = 0; i < 5; i++) {
                gamificationService.recordTrip(sofia.getId(), new TripCreateRequest(
                        TransportMode.BICYCLE, "Estación Los Jardines", -12.0150, -77.0050, "Estación Angamos", -12.1120, -77.0120, 14.5, 50
                ));
            }

            // Seed Carlos Trips
            gamificationService.recordTrip(carlos.getId(), new TripCreateRequest(
                    TransportMode.WALKING, "Av. Arequipa", -12.0720, -77.0350, "Centro Histórico", -12.0450, -77.0310, 5.2, 55
            ));
            gamificationService.recordTrip(carlos.getId(), new TripCreateRequest(
                    TransportMode.BICYCLE, "Surco", -12.1350, -77.0150, "San Borja Norte", -12.0910, -77.0020, 7.8, 26
            ));

            // Seed Lucia Trips
            gamificationService.recordTrip(lucia.getId(), new TripCreateRequest(
                    TransportMode.BICYCLE, "Barranco", -12.1480, -77.0210, "San Isidro Golf", -12.0950, -77.0420, 8.0, 28
            ));

            // Seed Diego Trips - Includes a SUSPICIOUS SPEED ANOMALY (e.g. 15 km walking in 10 min = 90 km/h)
            gamificationService.recordTrip(diego.getId(), new TripCreateRequest(
                    TransportMode.WALKING, "Av. Brasil", -12.0750, -77.0510, "Plaza Bolognesi", -12.0620, -77.0410, 15.0, 10
            ));
        }
    }

    private User createUser(String email, String password, String name, Role role, String seed, boolean hasBike) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(name);
        user.setRole(role);
        user.setAvatarUrl("https://api.dicebear.com/7.x/bottts/svg?seed=" + seed);
        user.setHasBicycle(hasBike);
        user = userRepository.save(user);
        userStatsRepository.save(new UserStats(user));
        return user;
    }
}
