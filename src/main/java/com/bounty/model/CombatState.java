package com.bounty.model;

import lombok.Data;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Live combat state stored in Redis. NOT a JPA entity.
 *
 * Extended with:
 *  - 5-move system: ATTACK, BOARD, EVADE, SABOTAGE, NEGOTIATE
 *  - Status effects on both sides (BLEEDING, ANCHORED, RATTLED, FORTIFIED)
 *  - Enemy stance (AGGRESSIVE, DEFENSIVE, DESPERATE) visible to player
 *  - Tide Pressure bar — fills each round; at 10 enemy unleashes a surge
 *  - Last roll result for dice-feel UI feedback
 */
@Data
public class CombatState implements Serializable {

    public enum Approach {
        ATTACK, BOARD, EVADE, SABOTAGE, NEGOTIATE
    }

    public enum Status {
        ONGOING, PLAYER_WON, PLAYER_LOST
    }

    public enum EnemyStance {
        AGGRESSIVE, DEFENSIVE, DESPERATE
    }

    public enum StatusEffect {
        BLEEDING,   // lose 5 HP per round
        ANCHORED,   // cannot EVADE next round
        RATTLED,    // -5 to all damage dealt
        FORTIFIED   // absorb 8 damage next hit
    }

    private String  playerId;
    private String  islandId;
    private int     playerHealth   = 100;
    private int     enemyHealth    = 100;
    private int     round          = 1;
    private int     bountyChange   = 0;
    private int     tidePressure   = 0;   // 0-10; at 10 → enemy surge
    private int     lastPlayerRoll = 0;   // for UI dice display
    private int     lastEnemyRoll  = 0;
    private boolean evadeActive    = false; // player used EVADE last round
    private boolean anchored       = false; // player is ANCHORED (can't evade)
    private Approach    playerApproach;
    private Status      status         = Status.ONGOING;
    private EnemyStance enemyStance    = EnemyStance.AGGRESSIVE;
    private List<StatusEffect> playerEffects = new ArrayList<>();
    private List<StatusEffect> enemyEffects  = new ArrayList<>();
    private String lastEventLine = ""; // last combat narrative line
}
