package com.hospital.billing.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Issues and verifies HS256-signed JWTs. The token carries the tenant (hospital) id, so the
 * tenant of an authenticated request can never be chosen by the client.
 */
@Service
public class JwtService {

    private static final String CLAIM_USER_ID = "uid";
    private static final String CLAIM_TENANT = "tid";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_NAME = "name";

    private final SecretKey signingKey;
    private final Duration expiration;
    private final Clock clock;

    public JwtService(JwtProperties properties, Clock clock) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
        this.expiration = Duration.ofMinutes(properties.expirationMinutes());
        this.clock = clock;
    }

    public record IssuedToken(String token, Instant expiresAt) {
    }

    public IssuedToken issue(AppUser user) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(expiration);
        String token = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.getUsername())
                .claim(CLAIM_USER_ID, user.getId())
                .claim(CLAIM_TENANT, user.getTenantId().toString())
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_NAME, user.getFullName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
        return new IssuedToken(token, expiresAt);
    }

    /** Returns the user for a valid, unexpired token; empty for anything else. */
    public Optional<AuthenticatedUser> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Number userId = claims.get(CLAIM_USER_ID, Number.class);
            return Optional.of(new AuthenticatedUser(
                    userId == null ? null : userId.longValue(),
                    claims.getSubject(),
                    claims.get(CLAIM_NAME, String.class),
                    UUID.fromString(claims.get(CLAIM_TENANT, String.class)),
                    Role.valueOf(claims.get(CLAIM_ROLE, String.class)),
                    claims.getId(),
                    claims.getExpiration() == null ? null : claims.getExpiration().toInstant()));
        } catch (JwtException | IllegalArgumentException | NullPointerException ex) {
            return Optional.empty();
        }
    }
}
