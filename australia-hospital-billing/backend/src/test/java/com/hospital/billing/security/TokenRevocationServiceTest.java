package com.hospital.billing.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenRevocationServiceTest {

    @Test
    void revokedTokenIsRejectedUntilItExpires() {
        Instant now = Instant.parse("2026-09-24T10:00:00Z");
        TokenRevocationService service = new TokenRevocationService(Clock.fixed(now, ZoneOffset.UTC));

        service.revoke("jti-1", now.plusSeconds(3600));

        assertTrue(service.isRevoked("jti-1"));
        assertFalse(service.isRevoked("jti-2"));
        assertFalse(service.isRevoked(null));
    }
}
