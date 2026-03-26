package com.bounty.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Tracks the active game season.  Season 1 is seeded automatically
 * when the server starts (see IslandService.seedInitialIslands).
 * The table is created automatically by Hibernate (ddl-auto=update).
 */
@Data
@Entity
@Table(name = "seasons")
public class Season {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /** Human-readable season number — Season 1, Season 2, etc. */
    @Column(nullable = false, unique = true)
    private Integer number;

    @Column(nullable = false, updatable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    /** Null while season is still active. */
    private LocalDateTime endedAt;

    /** Player UUID of the season champion — null until season ends. */
    private String championId;

    @Column(nullable = false)
    private Boolean active = true;
}
