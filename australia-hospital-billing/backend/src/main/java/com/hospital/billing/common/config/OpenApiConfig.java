package com.hospital.billing.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/** Swagger UI: http://localhost:8080/swagger-ui.html (click "Authorize" and paste the JWT). */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Australia Hospital Billing API",
                version = "v1",
                description = "Multi-hospital billing, Medicare/private-fund adjudication and claims lifecycle"),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
