package com.ecocommute.service;

import com.ecocommute.entity.Role;
import com.ecocommute.entity.User;
import com.ecocommute.repository.UserBadgeRepository;
import com.ecocommute.repository.UserRepository;
import com.ecocommute.repository.UserStatsRepository;
import com.ecocommute.security.GoogleTokenVerifierService;
import com.ecocommute.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserStatsRepository userStatsRepository;

    @Mock
    private UserBadgeRepository userBadgeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private GoogleTokenVerifierService googleTokenVerifierService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                userStatsRepository,
                userBadgeRepository,
                passwordEncoder,
                jwtService,
                googleTokenVerifierService
        );
    }

    @Test
    @DisplayName("Debe registrar un nuevo usuario exitosamente sin DTO")
    void testRegisterSuccess() {
        Map<String, Object> req = Map.of(
                "email", "test@ecocommute.org",
                "password", "Secret123!",
                "fullName", "Test User"
        );

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId("user-123");
            return u;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt.token.here");

        Map<String, Object> response = authService.register(req);

        assertNotNull(response);
        assertEquals("jwt.token.here", response.get("token"));
        assertEquals("test@ecocommute.org", response.get("email"));
        verify(userRepository, times(1)).save(any(User.class));
        verify(userStatsRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción si el email ya existe")
    void testRegisterDuplicateEmail() {
        Map<String, Object> req = Map.of(
                "email", "existing@ecocommute.org",
                "password", "Secret123!",
                "fullName", "Test User"
        );

        when(userRepository.existsByEmail("existing@ecocommute.org")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(req));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe iniciar sesión exitosamente con credenciales válidas")
    void testLoginSuccess() {
        Map<String, Object> req = Map.of(
                "email", "test@ecocommute.org",
                "password", "Secret123!"
        );

        User user = new User();
        user.setId("user-123");
        user.setEmail("test@ecocommute.org");
        user.setPassword("hashed_password");
        user.setRole(Role.ROLE_USER);

        when(userRepository.findByEmail("test@ecocommute.org")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Secret123!", "hashed_password")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt.token.here");

        Map<String, Object> response = authService.login(req);

        assertNotNull(response);
        assertEquals("jwt.token.here", response.get("token"));
        assertEquals("user-123", response.get("id"));
    }

    @Test
    @DisplayName("Debe fallar el inicio de sesión con contraseña incorrecta")
    void testLoginInvalidPassword() {
        Map<String, Object> req = Map.of(
                "email", "test@ecocommute.org",
                "password", "WrongPassword"
        );

        User user = new User();
        user.setEmail("test@ecocommute.org");
        user.setPassword("hashed_password");

        when(userRepository.findByEmail("test@ecocommute.org")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword", "hashed_password")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> authService.login(req));
    }
}
