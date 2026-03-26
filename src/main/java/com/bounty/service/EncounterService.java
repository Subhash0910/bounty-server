package com.bounty.service;

import com.bounty.model.*;
import com.bounty.model.CombatState.Approach;
import com.bounty.model.CombatState.EnemyStance;
import com.bounty.model.CombatState.Status;
import com.bounty.model.CombatState.StatusEffect;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class EncounterService {

    private static final String   COMBAT_PREFIX = "combat:";
    private static final Duration COMBAT_TTL    = Duration.ofMinutes(15);

    private final RedisTemplate<String, Object> redisTemplate;
    private final PlayerRepository    playerRepository;
    private final IslandRepository    islandRepository;
    private final EncounterRepository encounterRepository;
    private final IslandService       islandService;
    private final Random              rng = new Random();

    // ─────────────────────────────────────────────────────────────────────────
    // Resolve
    // ─────────────────────────────────────────────────────────────────────────
    private Player resolvePlayer(String email) {
        return playerRepository.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Player not found: " + email));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Start encounter
    // ─────────────────────────────────────────────────────────────────────────
    public CombatState startEncounter(String email, String islandId) {
        Player player = resolvePlayer(email);
        Island island = islandRepository.findById(islandId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Island not found: " + islandId));

        CombatState state = new CombatState();
        state.setPlayerId(player.getId());
        state.setIslandId(islandId);

        // Scale enemy HP slightly by island difficulty so higher islands feel harder
        int enemyBaseHp = 60 + island.getDifficulty() * 8; // diff 1→68, 5→100
        state.setEnemyHealth(Math.min(enemyBaseHp, 100));

        // Initial stance based on type
        state.setEnemyStance(switch (island.getType()) {
            case WARLORD -> EnemyStance.AGGRESSIVE;
            case VOID    -> EnemyStance.DESPERATE;
            default      -> EnemyStance.DEFENSIVE;
        });

        redisTemplate.opsForValue().set(COMBAT_PREFIX + player.getId(), state, COMBAT_TTL);
        return state;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Process turn
    // ─────────────────────────────────────────────────────────────────────────
    public CombatState processTurn(String email, Approach approach) {
        Player player = resolvePlayer(email);
        String key    = COMBAT_PREFIX + player.getId();

        CombatState state = (CombatState) redisTemplate.opsForValue().get(key);
        if (state == null)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No active combat. Sail to an island first.");
        if (state.getStatus() != Status.ONGOING)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Combat already finished.");

        // Refresh player
        player = playerRepository.findById(state.getPlayerId()).orElseThrow();
        Island island = islandRepository.findById(state.getIslandId()).orElseThrow();

        state.setPlayerApproach(approach);

        // ── 1. Apply existing status effects (bleeding etc.) ─────────────────
        applyDoTEffects(state);
        if (isOver(state)) { finish(state, player, island, key); return state; }

        // ── 2. Player action ─────────────────────────────────────────────────
        String eventLine = resolvePlayerAction(approach, state, player, island);
        state.setLastEventLine(eventLine);

        if (isOver(state)) { finish(state, player, island, key); return state; }

        // ── 3. Enemy counter-attack ──────────────────────────────────────────
        if (state.getStatus() == Status.ONGOING) {
            resolveEnemyAction(state, island);
        }

        // ── 4. Tide pressure ─────────────────────────────────────────────────
        if (state.getStatus() == Status.ONGOING) {
            state.setTidePressure(state.getTidePressure() + 1);
            if (state.getTidePressure() >= 10) {
                // Enemy surge — heavy hit
                int surge = 20 + rng.nextInt(11);
                boolean blocked = state.getPlayerEffects().contains(StatusEffect.FORTIFIED);
                if (blocked) {
                    surge = Math.max(0, surge - 8);
                    state.getPlayerEffects().remove(StatusEffect.FORTIFIED);
                }
                state.setPlayerHealth(Math.max(0, state.getPlayerHealth() - surge));
                state.setTidePressure(0);
                state.setLastEventLine(state.getLastEventLine()
                    + " | TIDE SURGE: " + surge + " damage crashes into you!");
            }
        }

        // ── 5. Update stance ─────────────────────────────────────────────────
        updateEnemyStance(state);

        // ── 6. Increment round ───────────────────────────────────────────────
        state.setRound(state.getRound() + 1);
        state.setEvadeActive(approach == Approach.EVADE);

        if (isOver(state)) {
            finish(state, player, island, key);
        } else {
            redisTemplate.opsForValue().set(key, state, COMBAT_TTL);
        }
        return state;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Player action resolver
    // ─────────────────────────────────────────────────────────────────────────
    private String resolvePlayerAction(Approach approach, CombatState state,
                                       Player player, Island island) {
        return switch (approach) {

            case ATTACK -> {
                int roll    = 14 + rng.nextInt(12);           // 14-25
                int debuff  = state.getPlayerEffects().contains(StatusEffect.RATTLED) ? 5 : 0;
                int dmg     = Math.max(1, roll - debuff);
                boolean crit = (roll >= 24);
                if (crit) dmg += 10;
                // Enemy fortified?
                if (state.getEnemyEffects().contains(StatusEffect.FORTIFIED)) {
                    dmg = Math.max(1, dmg - 8);
                    state.getEnemyEffects().remove(StatusEffect.FORTIFIED);
                }
                state.setEnemyHealth(Math.max(0, state.getEnemyHealth() - dmg));
                state.setLastPlayerRoll(roll);
                // Small chance to inflict BLEEDING on enemy
                if (rng.nextDouble() < 0.30) addEffect(state.getEnemyEffects(), StatusEffect.BLEEDING);
                state.getPlayerEffects().remove(StatusEffect.RATTLED);
                yield crit
                    ? "CRITICAL — " + dmg + " damage! Cannons split the air!"
                    : "You fire — " + dmg + " damage slams into their hull.";
            }

            case BOARD -> {
                // High risk, high reward — melee boarding action
                int roll      = 10 + rng.nextInt(16);         // 10-25
                int dmgDealt  = roll + 5;                     // 15-30
                int dmgTaken  = 8  + rng.nextInt(8);          // 8-15 (boarding is risky)
                if (state.getPlayerEffects().contains(StatusEffect.ANCHORED)) {
                    yield "You're ANCHORED — boarding is impossible this round!";
                }
                if (state.getEnemyEffects().contains(StatusEffect.FORTIFIED)) {
                    dmgDealt = Math.max(1, dmgDealt - 8);
                    state.getEnemyEffects().remove(StatusEffect.FORTIFIED);
                }
                state.setEnemyHealth(Math.max(0, state.getEnemyHealth() - dmgDealt));
                state.setPlayerHealth(Math.max(0, state.getPlayerHealth() - dmgTaken));
                state.setLastPlayerRoll(roll);
                // Boarding inflicts RATTLED on enemy
                addEffect(state.getEnemyEffects(), StatusEffect.RATTLED);
                yield "You board their ship — " + dmgDealt + " dealt, " + dmgTaken + " taken. Close quarters.";
            }

            case EVADE -> {
                // No damage, but dodge enemy next attack + chance to ANCHOR them
                if (state.isAnchored()) {
                    // ANCHORED players can't evade — it backfires
                    int backfire = 8 + rng.nextInt(6);
                    state.setPlayerHealth(Math.max(0, state.getPlayerHealth() - backfire));
                    state.setAnchored(false);
                    state.getPlayerEffects().remove(StatusEffect.ANCHORED);
                    yield "You tried to evade but you're ANCHORED — " + backfire + " damage from the chain snap!";
                }
                state.setEvadeActive(true);
                // 40% chance to ANCHOR enemy
                if (rng.nextDouble() < 0.40) {
                    addEffect(state.getEnemyEffects(), StatusEffect.ANCHORED);
                    yield "You break hard — evasion set. Enemy chain caught — they're ANCHORED!";
                }
                yield "You break hard to the side. Enemy shot misses. Evasion primed.";
            }

            case SABOTAGE -> {
                // Tactical: inflict RATTLED + ANCHORED, small damage, no retaliation this round
                if (player.getBounty() < 500) {
                    yield "Sabotage requires notoriety (500+ bounty). Not yet earned.";
                }
                int roll = 5 + rng.nextInt(8);                // 5-12 damage
                state.setEnemyHealth(Math.max(0, state.getEnemyHealth() - roll));
                addEffect(state.getEnemyEffects(), StatusEffect.RATTLED);
                if (rng.nextDouble() < 0.50) addEffect(state.getEnemyEffects(), StatusEffect.ANCHORED);
                state.setLastPlayerRoll(roll);
                yield "Sabotage lands — " + roll + " damage. Enemy RATTLED" +
                    (state.getEnemyEffects().contains(StatusEffect.ANCHORED) ? " and ANCHORED!" : ".");
            }

            case NEGOTIATE -> {
                if (rng.nextDouble() < 0.35) {
                    // Early win at half reward — mark won here so finish() handles it
                    state.setStatus(Status.PLAYER_WON);
                    int halfReward = island.getBountyReward() / 2;
                    state.setBountyChange(halfReward);
                    yield "They lower their flag. Half-bounty deal accepted — " + halfReward + " ₦.";
                } else {
                    int penalty = 20 + rng.nextInt(10);
                    state.setPlayerHealth(Math.max(0, state.getPlayerHealth() - penalty));
                    yield "They spit on your offer. " + penalty + " damage — you paid for that insult.";
                }
            }
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Enemy counter-attack
    // ─────────────────────────────────────────────────────────────────────────
    private void resolveEnemyAction(CombatState state, Island island) {
        // If player evaded this round, enemy misses
        if (state.isEvadeActive()) return;
        // If enemy is anchored, skip their attack
        if (state.getEnemyEffects().contains(StatusEffect.ANCHORED)) {
            state.getEnemyEffects().remove(StatusEffect.ANCHORED);
            return;
        }

        int base = switch (state.getEnemyStance()) {
            case AGGRESSIVE -> 12 + rng.nextInt(9);  // 12-20
            case DEFENSIVE  ->  6 + rng.nextInt(7);  //  6-12
            case DESPERATE  -> 18 + rng.nextInt(10); // 18-27
        };

        // Scale slightly with island difficulty
        base += (island.getDifficulty() - 1) * 2;

        // Player fortified?
        boolean blocked = state.getPlayerEffects().contains(StatusEffect.FORTIFIED);
        if (blocked) {
            base = Math.max(0, base - 8);
            state.getPlayerEffects().remove(StatusEffect.FORTIFIED);
        }

        state.setLastEnemyRoll(base);
        state.setPlayerHealth(Math.max(0, state.getPlayerHealth() - base));

        // Chance for enemy to apply effect on player based on stance
        if (state.getEnemyStance() == EnemyStance.AGGRESSIVE && rng.nextDouble() < 0.25)
            addEffect(state.getPlayerEffects(), StatusEffect.RATTLED);
        if (state.getEnemyStance() == EnemyStance.DESPERATE && rng.nextDouble() < 0.35)
            addEffect(state.getPlayerEffects(), StatusEffect.ANCHORED);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DoT / Status Tick
    // ─────────────────────────────────────────────────────────────────────────
    private void applyDoTEffects(CombatState state) {
        if (state.getPlayerEffects().contains(StatusEffect.BLEEDING)) {
            state.setPlayerHealth(Math.max(0, state.getPlayerHealth() - 5));
        }
        if (state.getEnemyEffects().contains(StatusEffect.BLEEDING)) {
            state.setEnemyHealth(Math.max(0, state.getEnemyHealth() - 5));
        }
        // BLEEDING clears after 3 rounds — simplification: 33% chance to clear each round
        if (rng.nextDouble() < 0.33) state.getPlayerEffects().remove(StatusEffect.BLEEDING);
        if (rng.nextDouble() < 0.33) state.getEnemyEffects().remove(StatusEffect.BLEEDING);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Enemy stance shift
    // ─────────────────────────────────────────────────────────────────────────
    private void updateEnemyStance(CombatState state) {
        int ehp = state.getEnemyHealth();
        if (ehp <= 20) {
            state.setEnemyStance(EnemyStance.DESPERATE);
        } else if (ehp <= 50) {
            // Alternate aggressive / defensive to keep it dynamic
            state.setEnemyStance(state.getRound() % 2 == 0
                ? EnemyStance.AGGRESSIVE : EnemyStance.DEFENSIVE);
        } else {
            state.setEnemyStance(state.getRound() % 3 == 0
                ? EnemyStance.DEFENSIVE : EnemyStance.AGGRESSIVE);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────
    private boolean isOver(CombatState state) {
        return state.getStatus() != Status.ONGOING
            || state.getPlayerHealth() <= 0
            || state.getEnemyHealth()  <= 0;
    }

    private void finish(CombatState state, Player player, Island island, String key) {
        redisTemplate.delete(key);
        boolean playerWon = state.getStatus() == Status.PLAYER_WON
            || (state.getEnemyHealth() <= 0 && state.getPlayerHealth() > 0);

        if (playerWon && state.getStatus() != Status.PLAYER_WON) {
            state.setStatus(Status.PLAYER_WON);
        } else if (!playerWon) {
            state.setStatus(Status.PLAYER_LOST);
        }

        if (state.getStatus() == Status.PLAYER_WON) {
            int reward = (int)(island.getBountyReward() * (1.0 + player.getIslandsConquered() * 0.1));
            if (state.getBountyChange() == 0) state.setBountyChange(reward);
            finaliseWin(player, island, state.getBountyChange(), state);
        } else {
            long penalty = (long)(player.getBounty() * 0.10);
            state.setBountyChange(-(int) penalty);
            finaliseLoss(player, island, state);
        }
    }

    private void finaliseWin(Player player, Island island, int reward, CombatState state) {
        player.setBounty(player.getBounty() + reward);
        player.setIslandsConquered(player.getIslandsConquered() + 1);
        playerRepository.save(player);
        islandService.updateOwner(island.getId(), player.getId());
        Encounter e = new Encounter();
        e.setPlayerId(player.getId()); e.setIslandId(island.getId());
        e.setOutcome(Outcome.WIN); e.setBountyChange(reward);
        e.setAiLore("Victory echoes across the waves as " + player.getHandle()
            + " plants their flag on " + island.getName() + ".");
        encounterRepository.save(e);
    }

    private void finaliseLoss(Player player, Island island, CombatState state) {
        long penalty = (long)(player.getBounty() * 0.10);
        player.setBounty(Math.max(0, player.getBounty() - penalty));
        playerRepository.save(player);
        Encounter e = new Encounter();
        e.setPlayerId(player.getId()); e.setIslandId(island.getId());
        e.setOutcome(Outcome.LOSE); e.setBountyChange(-(int) penalty);
        e.setAiLore("Defeated and adrift, " + player.getHandle()
            + " retreats from " + island.getName() + " with nothing but shame.");
        encounterRepository.save(e);
    }

    private void addEffect(List<StatusEffect> list, StatusEffect effect) {
        if (!list.contains(effect)) list.add(effect);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // History
    // ─────────────────────────────────────────────────────────────────────────
    public List<Encounter> getEncounterHistory(String email) {
        Player player = resolvePlayer(email);
        return encounterRepository.findByPlayerIdOrderByPlayedAtDesc(
            player.getId(), PageRequest.of(0, 10));
    }
}
