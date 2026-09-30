package com.hospital.billing.security;

import java.security.Principal;
import java.time.Instant;
import java.util.UUID;

/**
 * The principal stored in the SecurityContext, rebuilt from the JWT claims on each request.
 *
 * @param tokenId   the JWT id (jti), used to revoke this token on logout
 * @param expiresAt token expiry, so the revocation list can forget it afterwards
 */
public record AuthenticatedUser(Long userId, String username, String fullName, UUID tenantId, Role role,
                                String tokenId, Instant expiresAt) implements Principal {

    @Override
    public String getName() {
        return username;
    }
}
