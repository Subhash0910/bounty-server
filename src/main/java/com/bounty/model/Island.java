package com.bounty.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "islands")
public class Island {

    public enum IslandType { DRIFTER, MERCHANT, WARLORD, VOID }

    @Id
    private String id;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private IslandType type;

    @Column(nullable = false)
    private int difficulty;

    @Column(nullable = false)
    private int bountyReward;

    @Column(columnDefinition = "TEXT")
    private String lore;

    @Column(nullable = false)
    private float positionX;

    @Column(nullable = false)
    private float positionY;

    private String        ownerId;
    private LocalDateTime flagPlantedAt;

    @PrePersist
    public void prePersist() {
        if (id == null || id.isBlank()) {
            id = java.util.UUID.randomUUID().toString();
        }
    }
}
