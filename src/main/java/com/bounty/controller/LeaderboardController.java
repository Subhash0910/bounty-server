package com.bounty.controller;

import com.bounty.model.Player;
import com.bounty.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LeaderboardController {

    private final PlayerRepository playerRepository;

    /**
     * GET /api/leaderboard
     * Returns top 10 players sorted by bounty descending.
     * Used by WorldMapScene to render the captain leaderboard sidebar.
     */
    @GetMapping("/api/leaderboard")
    public ResponseEntity<List<Map<String, Object>>> getLeaderboard() {
        List<Player> top = playerRepository.findTop10ByOrderByBountyDesc();
        List<Map<String, Object>> result = top.stream()
            .map(p -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("handle", p.getHandle());
                m.put("bounty", p.getBounty());
                return m;
            })
            .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }
}
