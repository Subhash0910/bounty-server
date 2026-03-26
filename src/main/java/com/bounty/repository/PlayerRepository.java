package com.bounty.repository;

import com.bounty.model.Player;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerRepository extends MongoRepository<Player, String> {
    Optional<Player> findByEmail(String email);
    Optional<Player> findByHandle(String handle);

    // Top N players by bounty descending for leaderboard
    List<Player> findTop10ByOrderByBountyDesc();
}
