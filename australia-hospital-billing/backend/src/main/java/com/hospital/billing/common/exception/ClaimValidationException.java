package com.hospital.billing.common.exception;

import java.util.List;

/** Raised when a claim fails pre-submission checks; every failed rule is listed in details. */
public class ClaimValidationException extends DomainException {

    public ClaimValidationException(String message, List<String> violations) {
        super(ErrorCode.CLAIM_VALIDATION_FAILED, message, violations);
    }
}
