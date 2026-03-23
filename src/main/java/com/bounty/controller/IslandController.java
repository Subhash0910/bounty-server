package com.bounty.controller;

import com.bounty.model.Island;
import com.bounty.repository.PlayerRepository;
import com.bounty.service.IslandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
     * Requires JWT auth (enforced by SecurityConfig).
     */
    @GetMapping("/api/world/map")
    public ResponseEntity<List<Map<String, Object>>> getWorldMap() {
        List<Island> islands = islandService.getAllIslands();

        List<Map<String, Object>> result = islands.stream().map(island -> {
            String ownerHandle = null;
            if (island.getOwnerId() != null) {
                ownerHandle = playerRepository.findById(island.getOwnerId())
                    .map(p -> p.getHandle())
                    .orElse(null);
            }
            return Map.of(
                "id",           island.getId(),
                "name",         island.getName(),
                "type",         island.getType().name(),
                "difficulty",   island.getDifficulty(),
                "bountyReward", island.getBountyReward(),
                "ownerId",      island.getOwnerId()    != null ? island.getOwnerId()    : "",
                "ownerHandle",  ownerHandle             != null ? ownerHandle             : "",
                "positionX",    island.getPositionX(),
                "positionY",    island.getPositionY()
            );
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/islands/{id}
     * Returns full island details including lore and flagPlantedAt.
     * Requires JWT auth (enforced by SecurityConfig).
     */
    @GetMapping("/api/islands/{id}")
    public ResponseEntity<Map<String, Object>> getIslandById(@PathVariable String id) {
        Island island = islandService.getIslandById(id);

        String ownerHandle = null;
        if (island.getOwnerId() != null) {
            ownerHandle = playerRepository.findById(island.getOwnerId())
                .map(p -> p.getHandle())
                .orElse(null);
        }

        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("id",            island.getId());
        response.put("name",          island.getName());
        response.put("type",          island.getType().name());
        response.put("difficulty",    island.getDifficulty());
        response.put("bountyReward",  island.getBountyReward());
        response.put("lore",          island.getLore());
        response.put("ownerId",       island.getOwnerId());
        response.put("ownerHandle",   ownerHandle);
        response.put("flagPlantedAt", island.getFlagPlantedAt());
        response.put("positionX",     island.getPositionX());
        response.put("positionY",     island.getPositionY());

        return ResponseEntity.ok(response);
    }
}
