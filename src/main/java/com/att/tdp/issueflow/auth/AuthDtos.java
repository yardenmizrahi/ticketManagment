package com.att.tdp.issueflow.auth;

import com.att.tdp.issueflow.users.Role;
import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record LoginResponse(String accessToken, String tokenType, long expiresIn) {
    }

    public record MeResponse(Long id, String username, String email, String fullName, Role role) {
    }
}
