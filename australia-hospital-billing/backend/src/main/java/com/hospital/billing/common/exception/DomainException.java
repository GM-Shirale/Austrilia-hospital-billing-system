package com.hospital.billing.common.exception;

import java.util.List;

/**
 * Root of all business exceptions. Each subclass maps to exactly one {@link ErrorCode},
 * so the HTTP status is decided by the domain, not by if/else chains in controllers.
 */
public abstract class DomainException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<String> details;

    protected DomainException(ErrorCode errorCode, String message) {
        this(errorCode, message, List.of());
    }

    protected DomainException(ErrorCode errorCode, String message, List<String> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public List<String> getDetails() {
        return details;
    }
}
