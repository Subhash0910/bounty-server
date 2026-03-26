package com.bounty.repository;

import com.bounty.model.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SeasonRepository extends JpaRepository<Season, String> {

    /** Returns the currently active season — used by HUD / leaderboard. */
    Optional<Season> findByActiveTrue();
}
