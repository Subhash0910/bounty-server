package com.bounty.model;

import lombok.Data;
import java.io.Serializable;

/**
 * Live combat state stored in Redis. NOT a JPA entity.
 */
@Data
public class CombatState implements Serializable {

    public enum Approach {
        ATTACK, INTIMIDATE, NEGOTIATE
    }

    public enum Status {
        ONGOING, PLAYER_WON, PLAYER_LOST
    }

    private String playerId;
    private String islandId;
    private int playerHealth = 100;
    private int enemyHealth  = 100;
    private int round        = 1;
    private Approach playerApproach;
    private Status status    = Status.ONGOING;
}
