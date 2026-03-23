package com.bounty.model;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String handle;
    private Long bounty;
    private String tier;
}
