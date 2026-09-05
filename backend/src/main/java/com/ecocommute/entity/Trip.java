package com.ecocommute.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "trips")
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransportMode transportMode;

    private String originName;
    private double originLat;
    private double originLng;

    private String destinationName;
    private double destinationLat;
    private double destinationLng;

    private double distanceKm;
    private int durationMinutes;

    private double baselineCo2Grams;
    private double co2EmittedGrams;
    private double co2SavedGrams;
    private int caloriesBurned;
    private int pointsEarned;

    private boolean suspicious = false;
    private String suspiciousReason;

    private LocalDateTime completedAt = LocalDateTime.now();

    public Trip() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public TransportMode getTransportMode() { return transportMode; }
    public void setTransportMode(TransportMode transportMode) { this.transportMode = transportMode; }

    public String getOriginName() { return originName; }
    public void setOriginName(String originName) { this.originName = originName; }

    public double getOriginLat() { return originLat; }
    public void setOriginLat(double originLat) { this.originLat = originLat; }

    public double getOriginLng() { return originLng; }
    public void setOriginLng(double originLng) { this.originLng = originLng; }

    public String getDestinationName() { return destinationName; }
    public void setDestinationName(String destinationName) { this.destinationName = destinationName; }

    public double getDestinationLat() { return destinationLat; }
    public void setDestinationLat(double destinationLat) { this.destinationLat = destinationLat; }

    public double getDestinationLng() { return destinationLng; }
    public void setDestinationLng(double destinationLng) { this.destinationLng = destinationLng; }

    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public double getBaselineCo2Grams() { return baselineCo2Grams; }
    public void setBaselineCo2Grams(double baselineCo2Grams) { this.baselineCo2Grams = baselineCo2Grams; }

    public double getCo2EmittedGrams() { return co2EmittedGrams; }
    public void setCo2EmittedGrams(double co2EmittedGrams) { this.co2EmittedGrams = co2EmittedGrams; }

    public double getCo2SavedGrams() { return co2SavedGrams; }
    public void setCo2SavedGrams(double co2SavedGrams) { this.co2SavedGrams = co2SavedGrams; }

    public int getCaloriesBurned() { return caloriesBurned; }
    public void setCaloriesBurned(int caloriesBurned) { this.caloriesBurned = caloriesBurned; }

    public int getPointsEarned() { return pointsEarned; }
    public void setPointsEarned(int pointsEarned) { this.pointsEarned = pointsEarned; }

    public boolean isSuspicious() { return suspicious; }
    public void setSuspicious(boolean suspicious) { this.suspicious = suspicious; }

    public String getSuspiciousReason() { return suspiciousReason; }
    public void setSuspiciousReason(String suspiciousReason) { this.suspiciousReason = suspiciousReason; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
