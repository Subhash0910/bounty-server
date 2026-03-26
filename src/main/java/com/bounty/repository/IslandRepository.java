package com.bounty.repository;

import com.bounty.model.Island;
import com.bounty.model.Island.IslandType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IslandRepository extends JpaRepository<Island, String> {
    List<Island> findByType(IslandType type);
    List<Island> findByOwnerId(String ownerId);
    List<Island> findByDifficultyLessThanEqual(Integer difficulty);
    boolean existsByName(String name);
    Optional<Island> findByName(String name);
}
