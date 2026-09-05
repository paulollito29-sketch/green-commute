package com.ecocommute.service;

import com.ecocommute.entity.Role;
import com.ecocommute.entity.User;
import com.ecocommute.entity.UserStats;
import com.ecocommute.dto.auth.*;
import com.ecocommute.repository.UserBadgeRepository;
import com.ecocommute.repository.UserRepository;
import com.ecocommute.repository.UserStatsRepository;
import com.ecocommute.security.GoogleTokenVerifierService;
import com.ecocommute.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserStatsRepository userStatsRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleTokenVerifierService googleTokenVerifierService;

    public AuthService(UserRepository userRepository,
                       UserStatsRepository userStatsRepository,
                       UserBadgeRepository userBadgeRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       GoogleTokenVerifierService googleTokenVerifierService) {
        this.userRepository = userRepository;
        this.userStatsRepository = userStatsRepository;
        this.userBadgeRepository = userBadgeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.googleTokenVerifierService = googleTokenVerifierService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("El correo ya se encuentra registrado");
        }

        User user = new User();
        user.setEmail(request.email().toLowerCase().trim());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setRole(Role.ROLE_USER);
        user.setAuthProvider("LOCAL");
        user.setAvatarUrl("https://api.dicebear.com/7.x/bottts/svg?seed=" + user.getEmail());
        if (request.hasBicycle() != null) user.setHasBicycle(request.hasBicycle());
        if (request.maxWalkingMinutes() != null) user.setMaxWalkingMinutes(request.maxWalkingMinutes());

        user = userRepository.save(user);

        UserStats stats = new UserStats(user);
        userStatsRepository.save(stats);

        String token = jwtService.generateToken(user);
        return toAuthResponse(user, token);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().toLowerCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas"));

        if (!user.isActive()) {
            throw new IllegalStateException("Esta cuenta ha sido suspendida");
        }

        if (user.getPassword() == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("Credenciales inválidas");
        }

        String token = jwtService.generateToken(user);
        return toAuthResponse(user, token);
    }

    @Transactional
    public AuthResponse googleLogin(GoogleLoginRequest request) {
        GoogleTokenVerifierService.GoogleUserInfo googleUser = googleTokenVerifierService.verifyToken(request.idToken());
        if (googleUser == null) {
            throw new IllegalArgumentException("Token de Google inválido o expirado");
        }

        User user = userRepository.findByGoogleSub(googleUser.sub())
                .or(() -> userRepository.findByEmail(googleUser.email().toLowerCase()))
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(googleUser.email().toLowerCase());
                    newUser.setFullName(googleUser.name() != null ? googleUser.name() : "Usuario Google");
                    newUser.setGoogleSub(googleUser.sub());
                    newUser.setAuthProvider("GOOGLE");
                    newUser.setAvatarUrl(googleUser.pictureUrl() != null ? googleUser.pictureUrl() : "https://api.dicebear.com/7.x/bottts/svg?seed=" + googleUser.email());
                    newUser.setRole(Role.ROLE_USER);
                    User saved = userRepository.save(newUser);

                    UserStats stats = new UserStats(saved);
                    userStatsRepository.save(stats);
                    return saved;
                });

        if (!user.isActive()) {
            throw new IllegalStateException("Esta cuenta ha sido suspendida");
        }

        // Update google sub and avatar if missing
        if (user.getGoogleSub() == null) {
            user.setGoogleSub(googleUser.sub());
        }
        if (googleUser.pictureUrl() != null) {
            user.setAvatarUrl(googleUser.pictureUrl());
        }
        user = userRepository.save(user);

        String token = jwtService.generateToken(user);
        return toAuthResponse(user, token);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        UserStats stats = userStatsRepository.findByUserId(userId)
                .orElseGet(() -> new UserStats(user));

        List<UserProfileResponse.BadgeSummaryDTO> badges = userBadgeRepository.findByUserId(userId).stream()
                .map(ub -> new UserProfileResponse.BadgeSummaryDTO(
                        ub.getBadge().getId(),
                        ub.getBadge().getCode(),
                        ub.getBadge().getTitle(),
                        ub.getBadge().getDescription(),
                        ub.getBadge().getIconEmoji(),
                        ub.getAwardedAt().toString()
                ))
                .toList();

        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getAvatarUrl(),
                user.getRole(),
                user.getCurrentPoints(),
                user.getCurrentLevel(),
                user.getStreakDays(),
                user.isHasBicycle(),
                user.getMaxWalkingMinutes(),
                stats.getTotalCo2SavedKg(),
                stats.getTotalDistanceKm(),
                stats.getTotalTrips(),
                stats.getTotalCaloriesBurned(),
                stats.getTreesEquivalent(),
                badges
        );
    }

    private AuthResponse toAuthResponse(User user, String token) {
        return new AuthResponse(
                token,
                "Bearer",
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getAvatarUrl(),
                user.getRole(),
                user.getCurrentPoints(),
                user.getCurrentLevel(),
                user.getStreakDays()
        );
    }
}
