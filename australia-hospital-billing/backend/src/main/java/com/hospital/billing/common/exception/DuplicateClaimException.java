package com.hospital.billing.common.exception;

public class DuplicateClaimException extends DomainException {

    public DuplicateClaimException(String message) {
        super(ErrorCode.DUPLICATE_CLAIM, message);
    }
}
