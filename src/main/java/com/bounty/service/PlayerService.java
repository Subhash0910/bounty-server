package com.bounty.service;

import com.bounty.model.Player;
import com.bounty.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;

    public Player register(String handle, String email, String password) {
        if (playerRepository.existsByEmail(email))
            throw new RuntimeException("Email already in use");
        if (playerRepository.existsByHandle(handle))
            throw new RuntimeException("Handle already taken");

        Player player = new Player();
        player.setHandle(handle);
        player.setEmail(email);
        player.setPasswordHash(passwordEncoder.encode(password));
        return playerRepository.save(player);
    }

    public Player findByEmail(String email) {
        return playerRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Player not found"));
    }
}
