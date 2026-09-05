package com.ecocommute.repository;

import com.ecocommute.entity.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserBadgeRepository extends JpaRepository<UserBadge, String> {
    List<UserBadge> findByUserId(String userId);
    Optional<UserBadge> findByUserIdAndBadgeId(String userId, Long badgeId);
    boolean existsByUserIdAndBadgeId(String userId, Long badgeId);
}
