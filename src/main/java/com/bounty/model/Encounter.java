package com.bounty.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "encounters")
public class Encounter {

    public enum Outcome {
        WIN, LOSE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String playerId;

    @Column(nullable = false)
    private String islandId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Outcome outcome;

    @Column(nullable = false)
    private Integer bountyChange;

    @Column
    private String tidMarkFound; // nullable loot/lore reward

    @Lob
    @Column(columnDefinition = "TEXT")
    private String aiLore;

    @Column(nullable = false)
    private LocalDateTime playedAt = LocalDateTime.now();
}
