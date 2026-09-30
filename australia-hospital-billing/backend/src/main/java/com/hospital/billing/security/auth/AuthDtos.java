package com.hospital.billing.security.auth;

import com.hospital.billing.security.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "Username is required") @Size(max = 60) String username,
            @NotBlank(message = "Password is required") @Size(max = 100) String password) {
    }

    /** Password policy: 8+ characters with upper case, lower case, a digit and a symbol. */
    public static final String PASSWORD_POLICY = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,100}$";

    public record RegisterRequest(
            @NotNull(message = "Select a hospital") UUID hospitalId,
            @NotBlank(message = "Full name is required")
            @Size(min = 2, max = 120, message = "Full name must be 2-120 characters")
            @Pattern(regexp = "^[\\p{L}][\\p{L} .'-]*$", message = "Full name may only contain letters, spaces, apostrophes, dots and hyphens")
            String fullName,
            @NotBlank(message = "Username is required")
            @Pattern(regexp = "^[A-Za-z0-9._-]{4,60}$", message = "Username must be 4-60 characters: letters, digits, dot, underscore or hyphen")
            String username,
            @NotBlank(message = "Email is required") @Email(message = "Enter a valid email address") @Size(max = 120)
            String email,
            @NotBlank(message = "Password is required")
            @Pattern(regexp = PASSWORD_POLICY, message = "Password must be 8+ characters with upper and lower case letters, a number and a symbol")
            String password,
            @NotBlank(message = "Confirm your password") String confirmPassword,
            @NotNull(message = "Select a role") Role role) {
    }

    public record RegistrationOptions(boolean enabled, java.util.List<Role> roles) {
    }

    public record UserProfile(Long id, String username, String fullName, Role role,
                              UUID tenantId, String hospitalCode, String hospitalName) {
    }

    public record LoginResponse(String accessToken, String tokenType, Instant expiresAt, UserProfile user) {
    }
}
