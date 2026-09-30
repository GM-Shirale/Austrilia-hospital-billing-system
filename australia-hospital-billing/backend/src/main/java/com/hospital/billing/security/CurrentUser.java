package com.hospital.billing.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/** Static accessor for the signed-in user (used for audit notes, never for tenant filtering). */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Optional<AuthenticatedUser> get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public static String usernameOrSystem() {
        return get().map(AuthenticatedUser::username).orElse("system");
    }

    /** Database id of the signed-in user, or null for system actions (event consumers, migrations). */
    public static Long userIdOrNull() {
        return get().map(AuthenticatedUser::userId).orElse(null);
    }

    /** Name recorded in audit columns, e.g. "Priya Sharma (doctor)". */
    public static String displayName() {
        return get().map(u -> u.fullName() == null || u.fullName().isBlank()
                        ? u.username()
                        : "%s (%s)".formatted(u.fullName(), u.username()))
                .orElse("system");
    }
}
