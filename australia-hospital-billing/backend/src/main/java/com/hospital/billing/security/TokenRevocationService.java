package com.hospital.billing.security;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Logout for stateless JWTs: the token id (jti) is remembered until the token would have
 * expired anyway, and JwtAuthenticationFilter rejects revoked ids.
 *
 * <p>Kept in memory for a single instance. With several instances behind a load balancer
 * this list moves to Redis (SET jti EX ttl) so every node sees the logout.</p>
 */
@Service
public class TokenRevocationService {

    private final Map<String, Instant> revoked = new ConcurrentHashMap<>();
    private final Clock clock;

    public TokenRevocationService(Clock clock) {
        this.clock = clock;
    }

    public void revoke(String tokenId, Instant expiresAt) {
        if (tokenId == null) {
            return;
        }
        purgeExpired();
        revoked.put(tokenId, expiresAt == null ? clock.instant().plusSeconds(86_400) : expiresAt);
    }

    public boolean isRevoked(String tokenId) {
        return tokenId != null && revoked.containsKey(tokenId);
    }

    private void purgeExpired() {
        Instant now = clock.instant();
        revoked.values().removeIf(expiry -> expiry.isBefore(now));
    }
}
