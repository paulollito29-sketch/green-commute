package com.ecocommute.repository;

import com.ecocommute.entity.EmissionFactor;
import com.ecocommute.entity.TransportMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmissionFactorRepository extends JpaRepository<EmissionFactor, Long> {
    Optional<EmissionFactor> findByTransportMode(TransportMode transportMode);
}
