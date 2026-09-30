package com.hospital.billing.security.auth;

import com.hospital.billing.security.AuthenticatedUser;
import com.hospital.billing.security.TokenRevocationService;
import com.hospital.billing.common.exception.TenantAccessDeniedException;
import com.hospital.billing.security.auth.AuthDtos.LoginRequest;
import com.hospital.billing.security.auth.AuthDtos.RegisterRequest;
import com.hospital.billing.security.auth.AuthDtos.RegistrationOptions;
import com.hospital.billing.tenant.TenantContext;
import com.hospital.billing.security.auth.AuthDtos.LoginResponse;
import com.hospital.billing.security.auth.AuthDtos.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final TokenRevocationService tokenRevocationService;

    @Operation(summary = "Sign in and receive a JWT (send X-Tenant-ID to scope the login to a hospital)")
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "Roles that can be chosen on the registration screen")
    @GetMapping("/register/options")
    public RegistrationOptions registrationOptions() {
        return authService.registrationOptions();
    }

    @Operation(summary = "Create an account for a hospital and sign in (returns a JWT)")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse register(@Valid @RequestBody RegisterRequest request) {
        // The tenant must be bound before the transaction opens so Hibernate stamps tenant_id.
        TenantContext.getCurrentTenant().ifPresent(headerTenant -> {
            if (!headerTenant.equals(request.hospitalId())) {
                throw new TenantAccessDeniedException("X-Tenant-ID does not match the selected hospital");
            }
        });
        return TenantContext.callAs(request.hospitalId(), () -> authService.register(request));
    }

    @Operation(summary = "Profile of the signed-in user")
    @GetMapping("/me")
    public UserProfile me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return authService.currentProfile(principal);
    }

    @Operation(summary = "Sign out: the current JWT is revoked until it expires")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal AuthenticatedUser principal) {
        tokenRevocationService.revoke(principal.tokenId(), principal.expiresAt());
    }
}
