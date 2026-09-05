package com.ecocommute.service;

import com.ecocommute.entity.UserStats;
import com.ecocommute.dto.dashboard.LeaderboardEntryDTO;
import com.ecocommute.repository.UserStatsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class LeaderboardService {

    private final UserStatsRepository userStatsRepository;

    public LeaderboardService(UserStatsRepository userStatsRepository) {
        this.userStatsRepository = userStatsRepository;
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntryDTO> getLeaderboard() {
        List<UserStats> topStats = userStatsRepository.findTopEcoUsers();
        List<LeaderboardEntryDTO> result = new ArrayList<>();

        int rank = 1;
        for (UserStats stats : topStats) {
            result.add(new LeaderboardEntryDTO(
                    rank++,
                    stats.getUser().getId(),
                    stats.getUser().getFullName(),
                    stats.getUser().getAvatarUrl(),
                    stats.getUser().getCurrentPoints(),
                    Math.round(stats.getTotalCo2SavedKg() * 10.0) / 10.0,
                    stats.getTotalTrips(),
                    stats.getUser().getStreakDays(),
                    stats.getUser().getCurrentLevel()
            ));
        }

        return result;
    }
}
