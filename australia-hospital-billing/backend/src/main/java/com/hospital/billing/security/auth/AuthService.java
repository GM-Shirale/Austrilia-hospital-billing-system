package com.hospital.billing.security.auth;

import com.hospital.billing.common.exception.EntityNotFoundException;
import com.hospital.billing.security.AppUser;
import com.hospital.billing.security.AppUserRepository;
import com.hospital.billing.security.AuthenticatedUser;
import com.hospital.billing.security.JwtService;
import com.hospital.billing.common.exception.BusinessRuleException;
import com.hospital.billing.common.exception.DuplicateResourceException;
import com.hospital.billing.security.RegistrationProperties;
import com.hospital.billing.security.auth.AuthDtos.LoginRequest;
import com.hospital.billing.security.auth.AuthDtos.RegisterRequest;
import com.hospital.billing.security.auth.AuthDtos.RegistrationOptions;
import com.hospital.billing.security.auth.AuthDtos.LoginResponse;
import com.hospital.billing.security.auth.AuthDtos.UserProfile;
import com.hospital.billing.tenant.Hospital;
import com.hospital.billing.tenant.HospitalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /** Same message for "unknown user" and "wrong password": prevents username enumeration. */
    private static final String INVALID_CREDENTIALS = "Invalid username or password";

    private final AppUserRepository userRepository;
    private final HospitalRepository hospitalRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RegistrationProperties registrationProperties;

    /**
     * If the login screen sent X-Tenant-ID, the TenantFilter bound it and Hibernate only
     * searches users of that hospital: a Melbourne user cannot sign in to Sydney.
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        AppUser user = userRepository.findByUsernameIgnoreCase(request.username().trim())
                .filter(AppUser::isActive)
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> {
                    log.warn("Failed login attempt for username '{}'", request.username());
                    return new BadCredentialsException(INVALID_CREDENTIALS);
                });

        JwtService.IssuedToken token = jwtService.issue(user);
        log.info("User '{}' signed in to tenant {}", user.getUsername(), user.getTenantId());
        return new LoginResponse(token.token(), "Bearer", token.expiresAt(), profileOf(user));
    }

    public RegistrationOptions registrationOptions() {
        return new RegistrationOptions(registrationProperties.enabled(), registrationProperties.allowedRoles());
    }

    /**
     * Self-registration. The caller (AuthController) binds the chosen hospital to TenantContext
     * BEFORE this transaction starts, so Hibernate stamps tenant_id on the new user.
     * The password is stored as a BCrypt hash only; the response is a normal login (JWT).
     */
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (!registrationProperties.enabled()) {
            throw new AccessDeniedException("Self-registration is disabled; ask an administrator for an account");
        }
        if (!registrationProperties.allowedRoles().contains(request.role())) {
            throw new BusinessRuleException("Role %s cannot be chosen at registration".formatted(request.role()));
        }
        if (!request.password().equals(request.confirmPassword())) {
            throw new BusinessRuleException("Password and confirmation do not match");
        }
        Hospital hospital = hospitalRepository.findById(request.hospitalId())
                .filter(Hospital::isActive)
                .orElseThrow(() -> new EntityNotFoundException("Hospital", request.hospitalId()));

        String username = request.username().trim().toLowerCase();
        if (userRepository.countByUsernameAcrossTenants(username) > 0) {
            throw new DuplicateResourceException("Username '%s' is already taken".formatted(username));
        }
        String email = request.email().trim().toLowerCase();
        if (userRepository.countByEmailAcrossTenants(email) > 0) {
            throw new DuplicateResourceException("An account with email %s already exists".formatted(email));
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setFullName(request.fullName().trim().replaceAll("\\s+", " "));
        user.setEmail(email);
        user.setRole(request.role());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setActive(true);
        AppUser saved = userRepository.saveAndFlush(user);

        JwtService.IssuedToken token = jwtService.issue(saved);
        log.info("Registered user '{}' ({}) at {}", saved.getUsername(), saved.getRole(), hospital.getCode());
        return new LoginResponse(token.token(), "Bearer", token.expiresAt(), profileOf(saved));
    }

    @Transactional(readOnly = true)
    public UserProfile currentProfile(AuthenticatedUser principal) {
        AppUser user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new EntityNotFoundException("User", principal.userId()));
        return profileOf(user);
    }

    private UserProfile profileOf(AppUser user) {
        Hospital hospital = hospitalRepository.findById(user.getTenantId())
                .orElseThrow(() -> new EntityNotFoundException("Hospital", user.getTenantId()));
        return new UserProfile(user.getId(), user.getUsername(), user.getFullName(), user.getRole(),
                hospital.getId(), hospital.getCode(), hospital.getName());
    }
}
