package com.bounty.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "islands")
public class Island {

    public enum IslandType {
        DRIFTER, MERCHANT, WARLORD, VOID
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 60)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IslandType type;

    @Column(nullable = false)
    private Integer difficulty; // 1-5

    @Column(nullable = false)
    private Integer bountyReward;

    @Column
    private String ownerId; // nullable — no owner if unclaimed

    @Column
    private LocalDateTime flagPlantedAt;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String lore;

    @Column(nullable = false)
    private Float positionX;

    @Column(nullable = false)
    private Float positionY;
}
