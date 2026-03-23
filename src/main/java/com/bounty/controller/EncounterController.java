package com.bounty.controller;

import com.bounty.model.CombatState;
import com.bounty.model.CombatState.Approach;
import com.bounty.model.Encounter;
import com.bounty.service.EncounterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EncounterController {

    private final EncounterService encounterService;

    /**
     * POST /api/islands/{id}/sail
     * Starts a new combat encounter for the authenticated player.
     */
    @PostMapping("/api/islands/{id}/sail")
    public ResponseEntity<CombatState> sail(@PathVariable String id,
                                            Authentication auth) {
        String playerId = resolvePlayerId(auth);
        CombatState state = encounterService.startEncounter(playerId, id);
        return ResponseEntity.ok(state);
    }

    /**
     * POST /api/encounter/turn
     * Body: { "approach": "ATTACK" | "INTIMIDATE" | "NEGOTIATE" }
     */
    @PostMapping("/api/encounter/turn")
    public ResponseEntity<CombatState> turn(@RequestBody Map<String, String> body,
                                            Authentication auth) {
        String playerId = resolvePlayerId(auth);
        Approach approach = Approach.valueOf(body.get("approach").toUpperCase());
        CombatState state = encounterService.processTurn(playerId, approach);
        return ResponseEntity.ok(state);
    }

    /**
     * GET /api/encounter/history
     * Returns last 10 encounters for the authenticated player.
     */
    @GetMapping("/api/encounter/history")
    public ResponseEntity<List<Encounter>> history(Authentication auth) {
        String playerId = resolvePlayerId(auth);
        return ResponseEntity.ok(encounterService.getEncounterHistory(playerId));
    }

    /**
     * Extracts playerId from the JWT principal (email stored as principal name).
     * We resolve the actual player ID via PlayerRepository in service layer,
     * but here we pass the email; EncounterService.startEncounter accepts playerId.
     * To keep things simple we store player UUID in JWT subject instead of email.
     *
     * NOTE: JwtUtil currently sets email as subject. If you want to switch to UUID,
     * update JwtUtil.generateToken() to use player.getId() and update AuthController.
     * For now we pass the email as the identifier and resolve in EncounterService.
     */
    private String resolvePlayerId(Authentication auth) {
        // auth.getName() returns the JWT subject (currently email)
        return auth.getName();
    }
}
