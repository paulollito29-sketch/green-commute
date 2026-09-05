package com.ecocommute.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_stats")
public class UserStats {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    private double totalCo2SavedKg = 0.0;
    private double totalDistanceKm = 0.0;
    private int totalTrips = 0;
    private int totalCaloriesBurned = 0;

    private LocalDateTime updatedAt = LocalDateTime.now();

    public UserStats() {}

    public UserStats(User user) {
        this.user = user;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public double getTotalCo2SavedKg() { return totalCo2SavedKg; }
    public void setTotalCo2SavedKg(double totalCo2SavedKg) { this.totalCo2SavedKg = totalCo2SavedKg; }

    public double getTotalDistanceKm() { return totalDistanceKm; }
    public void setTotalDistanceKm(double totalDistanceKm) { this.totalDistanceKm = totalDistanceKm; }

    public int getTotalTrips() { return totalTrips; }
    public void setTotalTrips(int totalTrips) { this.totalTrips = totalTrips; }

    public int getTotalCaloriesBurned() { return totalCaloriesBurned; }
    public void setTotalCaloriesBurned(int totalCaloriesBurned) { this.totalCaloriesBurned = totalCaloriesBurned; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public double getTreesEquivalent() {
        return this.totalCo2SavedKg / 22.0;
    }
}
