package com.ecocommute.entity;

public enum TransportMode {
    CAR_SOLO("Vehículo / Auto Convencional", 170.0, 0.0, false, 0),
    BICYCLE("Bicicleta", 0.0, 3.0, true, 35),
    WALKING("Caminata", 0.0, 3.0, true, 40);

    private final String displayName;
    private final double co2GramsPerKm;
    private final double pointsMultiplier;
    private final boolean sustainable;
    private final int caloriesPerKm;

    TransportMode(String displayName, double co2GramsPerKm, double pointsMultiplier, boolean sustainable, int caloriesPerKm) {
        this.displayName = displayName;
        this.co2GramsPerKm = co2GramsPerKm;
        this.pointsMultiplier = pointsMultiplier;
        this.sustainable = sustainable;
        this.caloriesPerKm = caloriesPerKm;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getCo2GramsPerKm() {
        return co2GramsPerKm;
    }

    public double getPointsMultiplier() {
        return pointsMultiplier;
    }

    public boolean isSustainable() {
        return sustainable;
    }

    public int getCaloriesPerKm() {
        return caloriesPerKm;
    }
}
