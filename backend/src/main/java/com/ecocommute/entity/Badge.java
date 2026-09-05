package com.ecocommute.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "badges")
public class Badge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private String title;

    private String description;
    private String iconUrl;
    private String iconEmoji;

    private int requiredPoints;
    private double requiredCo2SavedKg;
    private int requiredStreakDays;
    private int requiredTrips;

    public Badge() {}

    public Badge(String code, String title, String description, String iconEmoji, int requiredPoints, double requiredCo2SavedKg, int requiredStreakDays, int requiredTrips) {
        this.code = code;
        this.title = title;
        this.description = description;
        this.iconEmoji = iconEmoji;
        this.requiredPoints = requiredPoints;
        this.requiredCo2SavedKg = requiredCo2SavedKg;
        this.requiredStreakDays = requiredStreakDays;
        this.requiredTrips = requiredTrips;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }

    public String getIconEmoji() { return iconEmoji; }
    public void setIconEmoji(String iconEmoji) { this.iconEmoji = iconEmoji; }

    public int getRequiredPoints() { return requiredPoints; }
    public void setRequiredPoints(int requiredPoints) { this.requiredPoints = requiredPoints; }

    public double getRequiredCo2SavedKg() { return requiredCo2SavedKg; }
    public void setRequiredCo2SavedKg(double requiredCo2SavedKg) { this.requiredCo2SavedKg = requiredCo2SavedKg; }

    public int getRequiredStreakDays() { return requiredStreakDays; }
    public void setRequiredStreakDays(int requiredStreakDays) { this.requiredStreakDays = requiredStreakDays; }

    public int getRequiredTrips() { return requiredTrips; }
    public void setRequiredTrips(int requiredTrips) { this.requiredTrips = requiredTrips; }
}
