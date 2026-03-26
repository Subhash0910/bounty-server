package com.bounty.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * A TideMark is a special power-up / passive ability a player discovers
 * after conquering an island.  Used by the Month-2 power system.
 * The table is created automatically by Hibernate (ddl-auto=update).
 */
@Data
@Entity
@Table(name = "tide_marks")
public class TideMark {

    public enum PowerType {
        OFFENSIVE,   // boosts attack damage
        DEFENSIVE,   // reduces incoming damage
        TACTICAL,    // improves negotiate / intimidate odds
        UTILITY      // passive bonuses (extra bounty, reduced penalty, etc.)
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String playerId;

    @Column(nullable = false, length = 100)
    private String name;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PowerType powerType;

    /** Only one TideMark can be equipped per player at a time. */
    @Column(nullable = false)
    private Boolean equipped = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime discoveredAt = LocalDateTime.now();
}
