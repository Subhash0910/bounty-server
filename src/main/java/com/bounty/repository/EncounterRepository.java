package com.bounty.repository;

import com.bounty.model.Encounter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface EncounterRepository extends JpaRepository<Encounter, String> {
    List<Encounter> findByPlayerIdOrderByPlayedAtDesc(String playerId, Pageable pageable);
}
