package com.ecocommute.service;

import com.ecocommute.entity.Role;
import com.ecocommute.entity.User;
import com.ecocommute.dto.auth.LoginRequest;
import com.ecocommute.dto.auth.RegisterRequest;
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
    @DisplayName("Debe registrar exitosamente un usuario nuevo con contraseña encriptada")
    void testRegisterSuccess() {
        RegisterRequest req = new RegisterRequest("nuevo@ecocommute.org", "Pass123!", "Nuevo Usuario", true, 15);

        when(userRepository.existsByEmail("nuevo@ecocommute.org")).thenReturn(false);
        when(passwordEncoder.encode("Pass123!")).thenReturn("$2a$10$hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId("user-uuid-123");
            return u;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("valid.jwt.token");

        var response = authService.register(req);

        assertNotNull(response);
        assertEquals("nuevo@ecocommute.org", response.email());
        assertEquals("valid.jwt.token", response.token());
        assertEquals(Role.ROLE_USER, response.role());
        verify(userRepository).save(any(User.class));
        verify(userStatsRepository).save(any());
    }

    @Test
    @DisplayName("Debe rechazar registro con correo duplicado")
    void testRegisterDuplicateEmail() {
        RegisterRequest req = new RegisterRequest("existente@ecocommute.org", "Pass123!", "Usuario", true, 15);
        when(userRepository.existsByEmail("existente@ecocommute.org")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(req));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe rechazar login con contraseña incorrecta")
    void testLoginInvalidPassword() {
        User user = new User("test@ecocommute.org", "$2a$10$hashedPassword", "Test User", Role.ROLE_USER);
        user.setActive(true);

        when(userRepository.findByEmail("test@ecocommute.org")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass", "$2a$10$hashedPassword")).thenReturn(false);

        LoginRequest req = new LoginRequest("test@ecocommute.org", "WrongPass");

        assertThrows(IllegalArgumentException.class, () -> authService.login(req));
    }

    @Test
    @DisplayName("Debe bloquear login si la cuenta está suspendida por el Administrador")
    void testLoginSuspendedAccount() {
        User user = new User("suspendido@ecocommute.org", "$2a$10$hashedPassword", "Suspended User", Role.ROLE_USER);
        user.setActive(false); // Suspended

        when(userRepository.findByEmail("suspendido@ecocommute.org")).thenReturn(Optional.of(user));

        LoginRequest req = new LoginRequest("suspendido@ecocommute.org", "Pass123!");

        assertThrows(IllegalStateException.class, () -> authService.login(req));
    }
}
