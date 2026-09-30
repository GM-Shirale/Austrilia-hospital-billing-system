package com.hospital.billing.tenant;

import com.hospital.billing.common.exception.ErrorCode;
import com.hospital.billing.common.exception.ProblemResponseWriter;
import com.hospital.billing.security.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Resolves the tenant for every HTTP request.
 *
 * <ol>
 *   <li>Reads the optional {@code X-Tenant-ID} header and validates it is a canonical UUID.</li>
 *   <li>For authenticated calls the tenant ALWAYS comes from the signed JWT; a header that
 *       names a different hospital is rejected with 403 (a user cannot switch hospitals by
 *       editing a header). A malformed header (e.g. a stale "undefined" from the browser) is
 *       ignored for signed-in users instead of failing the request with 400.</li>
 *   <li>For anonymous calls (the login screen) the header scopes the user lookup to the
 *       chosen hospital.</li>
 *   <li>The {@code finally} block clears the ThreadLocal so pooled threads never leak a
 *       tenant into the next request.</li>
 * </ol>
 *
 * Registered inside the Spring Security chain right after JWT authentication (see
 * SecurityConfig); deliberately NOT a @Component so it is not registered twice.
 */
public class TenantFilter extends OncePerRequestFilter {

    public static final String TENANT_HEADER = "X-Tenant-ID";
    private static final Pattern UUID_PATTERN =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private final ProblemResponseWriter problemResponseWriter;

    public TenantFilter(ProblemResponseWriter problemResponseWriter) {
        this.problemResponseWriter = problemResponseWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String header = request.getHeader(TENANT_HEADER);
            UUID tokenTenant = authenticatedTenant();
            UUID headerTenant = null;
            if (StringUtils.hasText(header) && !"null".equals(header.trim()) && !"undefined".equals(header.trim())) {
                if (UUID_PATTERN.matcher(header.trim()).matches()) {
                    headerTenant = UUID.fromString(header.trim());
                } else if (tokenTenant == null) {
                    // Only fatal for anonymous calls; for signed-in users the JWT decides the tenant.
                    problemResponseWriter.write(request, response, ErrorCode.INVALID_TENANT,
                            TENANT_HEADER + " must be a valid UUID");
                    return;
                }
            }

            if (tokenTenant != null) {
                if (headerTenant != null && !headerTenant.equals(tokenTenant)) {
                    problemResponseWriter.write(request, response, ErrorCode.TENANT_ACCESS_DENIED,
                            "You are not allowed to access data of hospital " + headerTenant);
                    return;
                }
                TenantContext.setCurrentTenant(tokenTenant);
            } else if (headerTenant != null) {
                TenantContext.setCurrentTenant(headerTenant);
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private UUID authenticatedTenant() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user.tenantId();
        }
        return null;
    }
}
