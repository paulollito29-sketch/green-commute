package com.ecocommute.service;

import com.ecocommute.entity.UserStats;
import com.ecocommute.repository.UserStatsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LeaderboardService {

    private final UserStatsRepository userStatsRepository;

    public LeaderboardService(UserStatsRepository userStatsRepository) {
        this.userStatsRepository = userStatsRepository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getLeaderboard() {
        List<UserStats> topStats = userStatsRepository.findTopEcoUsers();
        List<Map<String, Object>> result = new ArrayList<>();

        int rank = 1;
        for (UserStats s : topStats) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("userId", s.getUser().getId());
            entry.put("fullName", s.getUser().getFullName());
            entry.put("avatarUrl", s.getUser().getAvatarUrl());
            entry.put("totalCo2SavedKg", s.getTotalCo2SavedKg());
            entry.put("currentPoints", s.getUser().getCurrentPoints());
            entry.put("rank", rank++);
            result.add(entry);
        }
        return result;
    }
}
