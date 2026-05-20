package com.att.tdp.issueflow.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JwtService {
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final JwtProperties properties;
    private final ObjectMapper objectMapper;

    public String createToken(AppUserDetails details) {
        Instant now = Instant.now();
        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", details.user().getUsername());
        payload.put("userId", details.user().getId());
        payload.put("role", details.user().getRole().name());
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", now.plusSeconds(properties.expirationSeconds()).getEpochSecond());
        String unsigned = encode(header) + "." + encode(payload);
        return unsigned + "." + sign(unsigned);
    }

    public Optional<String> extractUsername(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3 || !sign(parts[0] + "." + parts[1]).equals(parts[2])) {
                return Optional.empty();
            }
            Map<String, Object> payload = objectMapper.readValue(
                    DECODER.decode(parts[1]), new TypeReference<>() {
                    });
            long exp = ((Number) payload.get("exp")).longValue();
            if (Instant.now().getEpochSecond() >= exp) {
                return Optional.empty();
            }
            return Optional.of((String) payload.get("sub"));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public long expirationSeconds() {
        return properties.expirationSeconds();
    }

    private String encode(Object value) {
        try {
            return ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to create JWT", ex);
        }
    }

    private String sign(String unsigned) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return ENCODER.encodeToString(mac.doFinal(unsigned.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign JWT", ex);
        }
    }
}
