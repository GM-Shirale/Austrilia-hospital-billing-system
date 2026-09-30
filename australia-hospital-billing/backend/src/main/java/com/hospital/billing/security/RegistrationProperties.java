package com.hospital.billing.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Self-registration policy (app.security.registration.*).
 *
 * @param enabled      false turns POST /api/v1/auth/register off (e.g. production, where an
 *                     administrator creates accounts)
 * @param allowedRoles roles a new user may pick on the registration screen
 */
@ConfigurationProperties(prefix = "app.security.registration")
public record RegistrationProperties(Boolean enabled, List<Role> allowedRoles) {

    public RegistrationProperties {
        enabled = enabled == null || enabled;
        allowedRoles = allowedRoles == null || allowedRoles.isEmpty()
                ? List.of(Role.RECEPTIONIST, Role.DOCTOR, Role.BILLING_OFFICER, Role.LAB, Role.PHARMACY, Role.ADMIN)
                : List.copyOf(allowedRoles);
    }
}
