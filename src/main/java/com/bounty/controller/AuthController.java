package com.bounty.controller;

import com.bounty.model.AuthRequest;
import com.bounty.model.AuthResponse;
import com.bounty.model.Player;
import com.bounty.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final PlayerService playerService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        Player player = playerService.register(
            request.getHandle(),
            request.getEmail(),
            request.getPassword()
        );
        return ResponseEntity.ok("Player registered: " + player.getHandle());
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        Player player = playerService.findByEmail(request.getEmail());
        return ResponseEntity.ok(
            new AuthResponse("token-placeholder", player.getHandle(),
                player.getBounty(), player.getTier())
        );
    }

    @lombok.Data
    static class RegisterRequest {
        private String handle;
        private String email;
        private String password;
    }
}
