package com.bounty.controller;

import com.bounty.config.JwtUtil;
import com.bounty.model.AuthRequest;
import com.bounty.model.AuthResponse;
import com.bounty.model.Player;
import com.bounty.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final PlayerService playerService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

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

        if (!passwordEncoder.matches(request.getPassword(), player.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }

        String token = jwtUtil.generateToken(player.getEmail());
        return ResponseEntity.ok(
            new AuthResponse(token, player.getHandle(), player.getBounty(), player.getTier())
        );
    }

    @lombok.Data
    static class RegisterRequest {
        private String handle;
        private String email;
        private String password;
    }
}
