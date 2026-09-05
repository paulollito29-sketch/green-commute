package com.ecocommute.service;

import com.ecocommute.entity.*;
import com.ecocommute.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final UserStatsRepository userStatsRepository;
    private final TripRepository tripRepository;
    private final EmissionFactorRepository emissionFactorRepository;

    public AdminService(UserRepository userRepository,
                        UserStatsRepository userStatsRepository,
                        TripRepository tripRepository,
                        EmissionFactorRepository emissionFactorRepository) {
        this.userRepository = userRepository;
        this.userStatsRepository = userStatsRepository;
        this.tripRepository = tripRepository;
        this.emissionFactorRepository = emissionFactorRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAdminKpis() {
        long totalUsers = userRepository.count();
        long totalTrips = tripRepository.count();
        double totalCo2 = userStatsRepository.sumTotalCo2SavedKg();
        long suspiciousCount = tripRepository.findBySuspiciousTrueOrderByCompletedAtDesc(PageRequest.of(0, 1)).getTotalElements();

        Map<String, Object> kpis = new HashMap<>();
        kpis.put("totalUsers", totalUsers);
        kpis.put("totalTrips", totalTrips);
        kpis.put("totalCo2SavedKg", totalCo2);
        kpis.put("suspiciousTripsCount", suspiciousCount);
        return kpis;
    }

    @Transactional(readOnly = true)
    public Page<User> getUsers(int page, int size, String search) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (search != null && !search.trim().isEmpty()) {
            return userRepository.findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(search.trim(), search.trim(), pageRequest);
        }
        return userRepository.findAll(pageRequest);
    }

    @Transactional
    public User toggleUserStatus(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        user.setActive(!user.isActive());
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Page<Trip> getSuspiciousTrips(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "completedAt"));
        return tripRepository.findBySuspiciousTrueOrderByCompletedAtDesc(pageRequest);
    }

    @Transactional(readOnly = true)
    public List<EmissionFactor> getEmissionFactors() {
        return emissionFactorRepository.findAll();
    }

    @Transactional
    public EmissionFactor updateEmissionFactor(Long id, double gramsCo2PerKm) {
        EmissionFactor factor = emissionFactorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Factor no encontrado"));
        factor.setGramsCo2PerKm(gramsCo2PerKm);
        return emissionFactorRepository.save(factor);
    }
}
