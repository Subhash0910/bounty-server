package com.bounty.repository;

import com.bounty.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, String> {
    Optional<Player> findByEmail(String email);
    Optional<Player> findByHandle(String handle);

    // Top 10 players by bounty descending — used by /api/leaderboard
    List<Player> findTop10ByOrderByBountyDesc();
}
