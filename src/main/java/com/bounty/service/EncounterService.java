package com.bounty.service;

import com.bounty.model.*;
import com.bounty.model.CombatState.Approach;
import com.bounty.model.CombatState.Status;
import com.bounty.model.Encounter.Outcome;
import com.bounty.repository.EncounterRepository;
import com.bounty.repository.IslandRepository;
import com.bounty.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class EncounterService {

    private static final String COMBAT_KEY_PREFIX = "combat:";
    private static final Duration COMBAT_TTL = Duration.ofMinutes(10);

    private final RedisTemplate<String, Object> redisTemplate;
    private final PlayerRepository playerRepository;
    private final IslandRepository islandRepository;
    private final EncounterRepository encounterRepository;
    private final IslandService islandService;

    // ── Resolve email → Player (Option B wiring fix) ─────────────────────────
    private Player resolvePlayer(String email) {
        return playerRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Player not found for email: " + email));
    }

    // ── Start ────────────────────────────────────────────────────────────────

    /**
     * @param email   JWT subject (player email) passed from controller
     * @param islandId  target island UUID
     */
    public CombatState startEncounter(String email, String islandId) {
        Player player = resolvePlayer(email);

        islandRepository.findById(islandId)
            .orElseThrow(() -> new RuntimeException("Island not found: " + islandId));

        CombatState state = new CombatState();
        state.setPlayerId(player.getId());   // store UUID in Redis state
        state.setIslandId(islandId);

        String key = COMBAT_KEY_PREFIX + player.getId();
        redisTemplate.opsForValue().set(key, state, COMBAT_TTL);
        return state;
    }

    // ── Process Turn ─────────────────────────────────────────────────────────

    /**
     * @param email   JWT subject (player email) passed from controller
     * @param approach player's chosen combat approach
     */
    public CombatState processTurn(String email, Approach approach) {
        Player player = resolvePlayer(email);

        String key = COMBAT_KEY_PREFIX + player.getId();
        CombatState state = (CombatState) redisTemplate.opsForValue().get(key);

        if (state == null)
            throw new RuntimeException("No active combat session. Start an encounter first.");
        if (state.getStatus() != Status.ONGOING)
            throw new RuntimeException("Combat already finished.");

        state.setPlayerApproach(approach);
        Random rng = new Random();

        // Refresh player from DB to get latest bounty/islandsConquered
        player = playerRepository.findById(state.getPlayerId())
            .orElseThrow(() -> new RuntimeException("Player not found"));

        // ── Damage Calculation ────────────────────────────────────────────────
        switch (approach) {
            case ATTACK -> {
                int dmgToEnemy  = 15 + rng.nextInt(11);  // 15-25
                int dmgToPlayer = 10 + rng.nextInt(11);  // 10-20
                state.setEnemyHealth(Math.max(0, state.getEnemyHealth()  - dmgToEnemy));
                state.setPlayerHealth(Math.max(0, state.getPlayerHealth() - dmgToPlayer));
            }
            case INTIMIDATE -> {
                int moraleHit = (player.getBounty() > 10_000) ? 30 : 10;
                state.setEnemyHealth(Math.max(0, state.getEnemyHealth() - moraleHit));
            }
            case NEGOTIATE -> {
                if (rng.nextDouble() < 0.40) {
                    state.setStatus(Status.PLAYER_WON);
                    Island island = islandRepository.findById(state.getIslandId()).orElseThrow();
                    int halfReward = island.getBountyReward() / 2;
                    finaliseWin(player, island, halfReward, state);
                    redisTemplate.delete(key);
                    return state;
                } else {
                    state.setPlayerHealth(Math.max(0, state.getPlayerHealth() - 25));
                }
            }
        }

        state.setRound(state.getRound() + 1);

        // ── Win / Lose Check ─────────────────────────────────────────────────
        if (state.getEnemyHealth() <= 0) {
            state.setStatus(Status.PLAYER_WON);
            Island island = islandRepository.findById(state.getIslandId()).orElseThrow();
            int fullReward = (int)(island.getBountyReward() * (1.0 + player.getIslandsConquered() * 0.1));
            finaliseWin(player, island, fullReward, state);
            redisTemplate.delete(key);
        } else if (state.getPlayerHealth() <= 0) {
            state.setStatus(Status.PLAYER_LOST);
            finaliseLoss(player, state);
            redisTemplate.delete(key);
        } else {
            redisTemplate.opsForValue().set(key, state, COMBAT_TTL);
        }

        return state;
    }

    // ── History ──────────────────────────────────────────────────────────────

    public List<Encounter> getEncounterHistory(String email) {
        Player player = resolvePlayer(email);
        return encounterRepository.findByPlayerIdOrderByPlayedAtDesc(
            player.getId(), PageRequest.of(0, 10));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void finaliseWin(Player player, Island island, int reward, CombatState state) {
        player.setBounty(player.getBounty() + reward);
        player.setIslandsConquered(player.getIslandsConquered() + 1);
        playerRepository.save(player);
        islandService.updateOwner(island.getId(), player.getId());

        Encounter encounter = new Encounter();
        encounter.setPlayerId(player.getId());
        encounter.setIslandId(island.getId());
        encounter.setOutcome(Outcome.WIN);
        encounter.setBountyChange(reward);
        encounter.setAiLore("Victory echoes across the waves as " + player.getHandle()
            + " plants their flag on " + island.getName() + ".");
        encounterRepository.save(encounter);
    }

    private void finaliseLoss(Player player, CombatState state) {
        long penalty = (long)(player.getBounty() * 0.10);
        player.setBounty(Math.max(0, player.getBounty() - penalty));
        playerRepository.save(player);

        Island island = islandRepository.findById(state.getIslandId()).orElseThrow();
        Encounter encounter = new Encounter();
        encounter.setPlayerId(player.getId());
        encounter.setIslandId(island.getId());
        encounter.setOutcome(Outcome.LOSE);
        encounter.setBountyChange(-(int) penalty);
        encounter.setAiLore("Defeated and adrift, " + player.getHandle()
            + " retreats from " + island.getName() + " with nothing but shame.");
        encounterRepository.save(encounter);
    }
}
