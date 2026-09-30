package com.hospital.billing.security;

import com.hospital.billing.common.exception.ErrorCode;
import com.hospital.billing.common.exception.ProblemResponseWriter;
import com.hospital.billing.common.web.CorrelationIdFilter;
import com.hospital.billing.tenant.TenantFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Stateless JWT security.
 * <ul>
 *   <li>URL rules only separate public from authenticated endpoints.</li>
 *   <li>Role rules live next to the code they protect, with {@code @PreAuthorize}
 *       (enabled by {@link EnableMethodSecurity}).</li>
 *   <li>Filter order: JwtAuthenticationFilter -> TenantFilter -> authorization.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/register/options",
            "/api/v1/hospitals",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/actuator/health/**",
            "/actuator/info",
            "/error"
    };

    private final JwtService jwtService;
    private final ProblemResponseWriter problemResponseWriter;
    private final CorsProperties corsProperties;
    private final TokenRevocationService tokenRevocationService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtService, tokenRevocationService);
        TenantFilter tenantFilter = new TenantFilter(problemResponseWriter);

        http
                .csrf(AbstractHttpConfigurer::disable)          // no cookies/sessions -> no CSRF risk
                .cors(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                problemResponseWriter.write(request, response, ErrorCode.AUTHENTICATION_FAILED,
                                        "Authentication is required. Please sign in again."))
                        .accessDeniedHandler((request, response, deniedException) ->
                                problemResponseWriter.write(request, response, ErrorCode.ACCESS_DENIED,
                                        "You do not have permission to perform this action")))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(tenantFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Patterns allow e.g. http://localhost:* so the Vite dev server works on any port (5173, 5174 ...)
        config.setAllowedOriginPatterns(corsProperties.allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", TenantFilter.TENANT_HEADER,
                CorrelationIdFilter.HEADER));
        config.setExposedHeaders(List.of(CorrelationIdFilter.HEADER));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
