package com.bounty.controller;

import com.bounty.model.Island;
import com.bounty.model.Player;
import com.bounty.repository.PlayerRepository;
import com.bounty.service.IslandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class IslandController {

    private final IslandService islandService;
    private final PlayerRepository playerRepository;

    /**
     * GET /api/world/map
     * Returns all islands enriched with owner handle (if owned).
     */
    @GetMapping("/api/world/map")
    public ResponseEntity<List<Map<String, Object>>> getWorldMap() {
        List<Island> islands = islandService.getAllIslands();
        List<Map<String, Object>> result = islands.stream().map(this::toMapSummary).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/islands/{id}
     * Returns full island details including lore and flagPlantedAt.
     */
    @GetMapping("/api/islands/{id}")
    public ResponseEntity<Map<String, Object>> getIslandById(@PathVariable String id) {
        Island island = islandService.getIslandById(id);
        return ResponseEntity.ok(toMapFull(island));
    }

    /**
     * POST /api/islands/{id}/claim
     * Directly claims an island for the authenticated player (no combat).
     * Useful for admin/testing; combat-based claiming goes through EncounterService.
     */
    @PostMapping("/api/islands/{id}/claim")
    public ResponseEntity<Map<String, Object>> claimIsland(@PathVariable String id,
                                                           Authentication auth) {
        String email = auth.getName(); // JWT subject = email
        Player player = playerRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Player not found"));

        Island updated = islandService.updateOwner(id, player.getId());
        return ResponseEntity.ok(toMapFull(updated));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String resolveOwnerHandle(String ownerId) {
        if (ownerId == null) return "";
        return playerRepository.findById(ownerId).map(Player::getHandle).orElse("");
    }

    private Map<String, Object> toMapSummary(Island island) {
        return Map.of(
            "id",           island.getId(),
            "name",         island.getName(),
            "type",         island.getType().name(),
            "difficulty",   island.getDifficulty(),
            "bountyReward", island.getBountyReward(),
            "ownerId",      island.getOwnerId()    != null ? island.getOwnerId() : "",
            "ownerHandle",  resolveOwnerHandle(island.getOwnerId()),
            "positionX",    island.getPositionX(),
            "positionY",    island.getPositionY()
        );
    }

    private Map<String, Object> toMapFull(Island island) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",            island.getId());
        m.put("name",          island.getName());
        m.put("type",          island.getType().name());
        m.put("difficulty",    island.getDifficulty());
        m.put("bountyReward",  island.getBountyReward());
        m.put("lore",          island.getLore());
        m.put("ownerId",       island.getOwnerId());
        m.put("ownerHandle",   resolveOwnerHandle(island.getOwnerId()));
        m.put("flagPlantedAt", island.getFlagPlantedAt());
        m.put("positionX",     island.getPositionX());
        m.put("positionY",     island.getPositionY());
        return m;
    }
}
