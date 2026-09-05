package com.ecocommute.repository;

import com.ecocommute.entity.UserStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserStatsRepository extends JpaRepository<UserStats, String> {

    Optional<UserStats> findByUserId(String userId);

    @Query("SELECT s FROM UserStats s JOIN FETCH s.user u WHERE u.active = true ORDER BY s.totalCo2SavedKg DESC")
    List<UserStats> findTopEcoUsers();

    @Query("SELECT COALESCE(SUM(s.totalCo2SavedKg), 0.0) FROM UserStats s")
    double sumTotalCo2SavedKg();

    @Query("SELECT COALESCE(SUM(s.totalDistanceKm), 0.0) FROM UserStats s")
    double sumTotalDistanceKm();

    @Query("SELECT COALESCE(SUM(s.totalTrips), 0) FROM UserStats s")
    long sumTotalTrips();
}
