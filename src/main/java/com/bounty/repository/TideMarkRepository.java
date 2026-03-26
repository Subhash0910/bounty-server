package com.bounty.repository;

import com.bounty.model.TideMark;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TideMarkRepository extends JpaRepository<TideMark, String> {

    /** All tide marks a player has discovered. */
    List<TideMark> findByPlayerId(String playerId);

    /** The currently equipped tide mark for a player (at most one). */
    Optional<TideMark> findByPlayerIdAndEquippedTrue(String playerId);
}
