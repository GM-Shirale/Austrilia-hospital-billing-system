package com.hospital.billing.common.exception;

public class DuplicateResourceException extends DomainException {

    public DuplicateResourceException(String message) {
        super(ErrorCode.DUPLICATE_RESOURCE, message);
    }
}
