package com.bounty.repository;

import com.bounty.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, String> {
    Optional<Player> findByEmail(String email);
    Optional<Player> findByHandle(String handle);
    boolean existsByEmail(String email);
    boolean existsByHandle(String handle);
}
