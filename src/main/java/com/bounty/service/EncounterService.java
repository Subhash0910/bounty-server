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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

    // ── Resolve email → Player ────────────────────────────────────────────────
    private Player resolvePlayer(String email) {
        return playerRepository.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Player not found for email: " + email + ". Please re-register or log in again."
            ));
    }

    // ── Start ────────────────────────────────────────────────────────────────
    public CombatState startEncounter(String email, String islandId) {
        Player player = resolvePlayer(email);

        islandRepository.findById(islandId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Island not found: " + islandId
            ));

        CombatState state = new CombatState();
        state.setPlayerId(player.getId());
        state.setIslandId(islandId);

        String key = COMBAT_KEY_PREFIX + player.getId();
        redisTemplate.opsForValue().set(key, state, COMBAT_TTL);
        return state;
    }

    // ── Process Turn ─────────────────────────────────────────────────────────
    public CombatState processTurn(String email, Approach approach) {
        Player player = resolvePlayer(email);

        String key = COMBAT_KEY_PREFIX + player.getId();
        CombatState state = (CombatState) redisTemplate.opsForValue().get(key);

        if (state == null)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No active combat session. Start an encounter first.");
        if (state.getStatus() != Status.ONGOING)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Combat already finished.");

        state.setPlayerApproach(approach);
        Random rng = new Random();

        // Refresh player to get latest bounty / islandsConquered
        player = playerRepository.findById(state.getPlayerId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));

        // ── Damage Calculation ────────────────────────────────────────────────
        switch (approach) {
            case ATTACK -> {
                int dmgToEnemy  = 15 + rng.nextInt(11);  // 15-25
                int dmgToPlayer = 10 + rng.nextInt(11);  // 10-20
                state.setEnemyHealth(Math.max(0, state.getEnemyHealth()  - dmgToEnemy));
                state.setPlayerHealth(Math.max(0, state.getPlayerHealth() - dmgToPlayer));
            }
            case INTIMIDATE -> {
                // Requires 1000+ bounty; higher bounty = 30 morale damage, otherwise 10
                int moraleHit = (player.getBounty() > 10_000) ? 30 : 10;
                state.setEnemyHealth(Math.max(0, state.getEnemyHealth() - moraleHit));
            }
            case NEGOTIATE -> {
                if (rng.nextDouble() < 0.40) {
                    // Early win at half reward
                    Island island = islandRepository.findById(state.getIslandId()).orElseThrow();
                    int halfReward = island.getBountyReward() / 2;
                    state.setStatus(Status.PLAYER_WON);
                    state.setBountyChange(halfReward);  // ← set for client overlay
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
            Island island = islandRepository.findById(state.getIslandId()).orElseThrow();
            int fullReward = (int)(island.getBountyReward() * (1.0 + player.getIslandsConquered() * 0.1));
            state.setStatus(Status.PLAYER_WON);
            state.setBountyChange(fullReward);  // ← set for client overlay
            finaliseWin(player, island, fullReward, state);
            redisTemplate.delete(key);
        } else if (state.getPlayerHealth() <= 0) {
            state.setStatus(Status.PLAYER_LOST);
            // Compute penalty before calling finaliseLoss so we can set it on state
            long penalty = (long)(player.getBounty() * 0.10);
            state.setBountyChange(-(int) penalty);  // ← set for client overlay (negative)
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
