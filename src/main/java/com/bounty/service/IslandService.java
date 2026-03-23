package com.bounty.service;

import com.bounty.model.Island;
import com.bounty.model.Island.IslandType;
import com.bounty.repository.IslandRepository;
import com.bounty.repository.PlayerRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class IslandService {

    private final IslandRepository islandRepository;
    private final PlayerRepository playerRepository;

    public List<Island> getAllIslands() {
        return islandRepository.findAll();
    }

    public Island getIslandById(String id) {
        return islandRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Island not found: " + id));
    }

    public Island updateOwner(String islandId, String playerId) {
        Island island = getIslandById(islandId);
        island.setOwnerId(playerId);
        island.setFlagPlantedAt(LocalDateTime.now());
        return islandRepository.save(island);
    }

    @PostConstruct
    public void seedInitialIslands() {
        if (islandRepository.count() > 0) return;

        Random rng = new Random(42);

        Object[][] seeds = {
            {"Ashen Cove",        IslandType.DRIFTER,  1, 120,  "A quiet bay where drifters wash ashore, forgotten by the world."},
            {"Saltwind Market",   IslandType.MERCHANT, 2, 250,  "Merchants haggle over stolen cargo under sun-bleached sails."},
            {"Iron Skull Rock",   IslandType.WARLORD,  4, 800,  "The warlord's banner flies high; challengers rarely return."},
            {"The Void Rift",     IslandType.VOID,     5, 2000, "An island that shouldn't exist \u2014 reality frays at its edges."},
            {"Copper Atoll",      IslandType.MERCHANT, 2, 300,  "Trade routes converge here; gold changes hands by moonlight."},
            {"Stormbreak Isle",   IslandType.DRIFTER,  1, 100,  "Perpetual storms keep most away, but survivors thrive."},
            {"Crimson Bastion",   IslandType.WARLORD,  3, 600,  "Walls of red stone built from the bones of defeated rivals."},
            {"Mirrorwater Shelf", IslandType.VOID,     5, 1800, "The sea here reflects a sky that no one recognises."},
            {"Driftwood Quay",    IslandType.DRIFTER,  2, 150,  "Makeshift docks hold the vessels of those with nowhere else to go."},
            {"Greytide Fortress", IslandType.WARLORD,  4, 900,  "Whoever holds Greytide controls the northern passage."},
        };

        for (Object[] row : seeds) {
            Island island = new Island();
            island.setName((String) row[0]);
            island.setType((IslandType) row[1]);
            island.setDifficulty((Integer) row[2]);
            island.setBountyReward((Integer) row[3]);
            island.setLore((String) row[4]);
            island.setPositionX((float)(rng.nextFloat() * 1000));
            island.setPositionY((float)(rng.nextFloat() * 1000));
            islandRepository.save(island);
        }
    }
}
