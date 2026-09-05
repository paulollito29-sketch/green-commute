package com.ecocommute.security;

import com.ecocommute.entity.Role;
import com.ecocommute.entity.User;
import com.ecocommute.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Endpoints públicos deben ser accesibles sin token (index.html, /api/v1/routes/plan)")
    void testPublicEndpointsAccess() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/dashboard/community-impact"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCo2SavedTons").exists());
    }

    @Test
    @DisplayName("Acceso anónimo a /api/v1/admin/dashboard/kpis debe ser rechazado con 401 Unauthorized")
    void testAnonymousAccessToAdminDenied() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard/kpis"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Usuario con ROLE_USER debe ser rechazado con 403 al intentar acceder a /api/v1/admin/**")
    void testUserRoleAccessToAdminDenied() throws Exception {
        User normalUser = userRepository.findByEmail("demo@ecocommute.org").orElseThrow();
        String userToken = jwtService.generateToken(normalUser);

        mockMvc.perform(get("/api/v1/admin/dashboard/kpis")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Usuario con ROLE_ADMIN debe tener acceso 200 OK a /api/v1/admin/**")
    void testAdminRoleAccessGranted() throws Exception {
        User adminUser = userRepository.findByEmail("admin@ecocommute.org").orElseThrow();
        String adminToken = jwtService.generateToken(adminUser);

        mockMvc.perform(get("/api/v1/admin/dashboard/kpis")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRegisteredUsers").exists());
    }

    @Test
    @DisplayName("Token JWT manipulado o inválido debe ser rechazado")
    void testTamperedJwtRejected() throws Exception {
        String fakeToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid.payload";

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + fakeToken))
                .andExpect(status().isUnauthorized());
    }
}
