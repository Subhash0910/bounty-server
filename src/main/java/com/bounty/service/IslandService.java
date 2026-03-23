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

    /**
     * Seeds 10 islands with hand-tuned positions spread across the full
     * [0, 1000] x [0, 1000] grid.  Positions are spaced at least 150 units
     * apart so they never overlap on the world map canvas.
     *
     * IMPORTANT: This only runs once.  To re-seed, truncate the islands table
     * and restart the server:
     *   TRUNCATE TABLE islands RESTART IDENTITY CASCADE;
     */
    @PostConstruct
    public void seedInitialIslands() {
        if (islandRepository.count() > 0) return;

        // { name, type, difficulty, bountyReward, lore, posX, posY }
        Object[][] seeds = {
            // Top-left quadrant
            {"Ashen Cove",        IslandType.DRIFTER,  1, 120,  "A quiet bay where drifters wash ashore, forgotten by the world.",                     80f,  90f},
            {"Saltwind Market",   IslandType.MERCHANT, 2, 250,  "Merchants haggle over stolen cargo under sun-bleached sails.",                       280f, 160f},
            // Top-right quadrant
            {"Raven Cove",        IslandType.DRIFTER,  1, 130,  "Crows circle endlessly; sailors say they carry the souls of the lost.",              650f,  80f},
            {"Copper Atoll",      IslandType.MERCHANT, 2, 300,  "Trade routes converge here; gold changes hands by moonlight.",                       850f, 170f},
            // Middle band
            {"Stormbreak Isle",   IslandType.DRIFTER,  2, 150,  "Perpetual storms keep most away, but survivors thrive.",                             140f, 400f},
            {"Crimson Bastion",   IslandType.WARLORD,  3, 600,  "Walls of red stone built from the bones of defeated rivals.",                        440f, 340f},
            {"Driftwood Quay",    IslandType.DRIFTER,  2, 150,  "Makeshift docks hold the vessels of those with nowhere else to go.",                  680f, 450f},
            // Bottom band
            {"Iron Skull Rock",   IslandType.WARLORD,  4, 800,  "The warlord's banner flies high; challengers rarely return.",                        230f, 700f},
            {"Greytide Fortress", IslandType.WARLORD,  4, 900,  "Whoever holds Greytide controls the northern passage.",                              780f, 720f},
            // Center-deep
            {"The Void Rift",     IslandType.VOID,     5, 2000, "An island that shouldn't exist \u2014 reality frays at its edges.",                  480f, 820f},
            // Extra islands (second wave, well-spaced)
            {"Mirrorwater Shelf", IslandType.VOID,     5, 1800, "The sea here reflects a sky that no one recognises.",                                 90f, 620f},
            {"Blacksand Port",    IslandType.MERCHANT, 3, 450,  "A black market where even the most wanted can find safe harbour.",                   940f, 530f},
        };

        for (Object[] row : seeds) {
            Island island = new Island();
            island.setName((String)     row[0]);
            island.setType((IslandType) row[1]);
            island.setDifficulty((Integer) row[2]);
            island.setBountyReward((Integer) row[3]);
            island.setLore((String)     row[4]);
            island.setPositionX((Float) row[5]);
            island.setPositionY((Float) row[6]);
            islandRepository.save(island);
        }
    }
}
