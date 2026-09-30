package com.hospital.billing.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Stable, machine-readable error codes returned in every error response.
 * The frontend switches on these, never on message text.
 */
public enum ErrorCode {
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Validation failed"),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "Malformed request"),
    INVALID_TENANT(HttpStatus.BAD_REQUEST, "Invalid tenant identifier"),
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "Authentication failed"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "Access denied"),
    TENANT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "Tenant access denied"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed"),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "Duplicate resource"),
    DUPLICATE_CLAIM(HttpStatus.CONFLICT, "Duplicate claim"),
    INVALID_STATE_TRANSITION(HttpStatus.CONFLICT, "Invalid state transition"),
    CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "Concurrent modification"),
    DATA_INTEGRITY_VIOLATION(HttpStatus.CONFLICT, "Data integrity violation"),
    CLAIM_VALIDATION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "Claim validation failed"),
    BUSINESS_RULE_VIOLATION(HttpStatus.UNPROCESSABLE_ENTITY, "Business rule violation"),
    MESSAGING_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Messaging unavailable"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");

    private final HttpStatus status;
    private final String title;

    ErrorCode(HttpStatus status, String title) {
        this.status = status;
        this.title = title;
    }

    public HttpStatus status() {
        return status;
    }

    public String title() {
        return title;
    }
}
