package com.att.tdp.issueflow.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "issueflow.jwt")
public record JwtProperties(String secret, long expirationSeconds) {
}
