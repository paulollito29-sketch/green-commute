package com.ecocommute.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "emission_factors")
public class EmissionFactor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false)
    private TransportMode transportMode;

    @Column(nullable = false)
    private double gramsCo2PerKm;

    private String description;
    private LocalDateTime updatedAt = LocalDateTime.now();

    public EmissionFactor() {}

    public EmissionFactor(TransportMode transportMode, double gramsCo2PerKm, String description) {
        this.transportMode = transportMode;
        this.gramsCo2PerKm = gramsCo2PerKm;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TransportMode getTransportMode() { return transportMode; }
    public void setTransportMode(TransportMode transportMode) { this.transportMode = transportMode; }

    public double getGramsCo2PerKm() { return gramsCo2PerKm; }
    public void setGramsCo2PerKm(double gramsCo2PerKm) { this.gramsCo2PerKm = gramsCo2PerKm; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
