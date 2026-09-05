package com.ecocommute.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    @Column(nullable = false)
    private String fullName;

    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.ROLE_USER;

    private boolean active = true;

    private String authProvider = "LOCAL"; // "LOCAL" or "GOOGLE"
    private String googleSub;

    private int currentPoints = 0;
    private int currentLevel = 1;
    private int streakDays = 0;
    private LocalDateTime lastTripDate;

    private boolean hasBicycle = true;
    private int maxWalkingMinutes = 20;

    private LocalDateTime createdAt = LocalDateTime.now();

    public User() {}

    public User(String email, String password, String fullName, Role role) {
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getAuthProvider() { return authProvider; }
    public void setAuthProvider(String authProvider) { this.authProvider = authProvider; }

    public String getGoogleSub() { return googleSub; }
    public void setGoogleSub(String googleSub) { this.googleSub = googleSub; }

    public int getCurrentPoints() { return currentPoints; }
    public void setCurrentPoints(int currentPoints) { this.currentPoints = currentPoints; }

    public int getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(int currentLevel) { this.currentLevel = currentLevel; }

    public int getStreakDays() { return streakDays; }
    public void setStreakDays(int streakDays) { this.streakDays = streakDays; }

    public LocalDateTime getLastTripDate() { return lastTripDate; }
    public void setLastTripDate(LocalDateTime lastTripDate) { this.lastTripDate = lastTripDate; }

    public boolean isHasBicycle() { return hasBicycle; }
    public void setHasBicycle(boolean hasBicycle) { this.hasBicycle = hasBicycle; }

    public int getMaxWalkingMinutes() { return maxWalkingMinutes; }
    public void setMaxWalkingMinutes(int maxWalkingMinutes) { this.maxWalkingMinutes = maxWalkingMinutes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
