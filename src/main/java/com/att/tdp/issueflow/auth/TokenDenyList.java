package com.att.tdp.issueflow.auth;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenDenyList {
    private final Map<String, Instant> tokens = new ConcurrentHashMap<>();

    public void deny(String token, long seconds) {
        tokens.put(token, Instant.now().plusSeconds(seconds));
    }

    public boolean isDenied(String token) {
        Instant expiresAt = tokens.get(token);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt.isBefore(Instant.now())) {
            tokens.remove(token);
            return false;
        }
        return true;
    }
}
