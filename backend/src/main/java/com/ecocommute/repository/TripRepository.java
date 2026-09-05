package com.ecocommute.repository;

import com.ecocommute.entity.TransportMode;
import com.ecocommute.entity.Trip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, String> {

    List<Trip> findByUserIdOrderByCompletedAtDesc(String userId);

    Page<Trip> findByUserIdOrderByCompletedAtDesc(String userId, Pageable pageable);

    Page<Trip> findBySuspiciousTrueOrderByCompletedAtDesc(Pageable pageable);

    @Query("SELECT t FROM Trip t WHERE t.user.id = :userId AND t.completedAt >= :since ORDER BY t.completedAt ASC")
    List<Trip> findUserTripsSince(@Param("userId") String userId, @Param("since") LocalDateTime since);

    @Query("SELECT t.transportMode, COUNT(t) FROM Trip t GROUP BY t.transportMode")
    List<Object[]> countTripsByTransportMode();
}
