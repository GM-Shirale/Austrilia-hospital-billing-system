package com.hospital.billing.common.exception;

public class TenantAccessDeniedException extends DomainException {

    public TenantAccessDeniedException(String message) {
        super(ErrorCode.TENANT_ACCESS_DENIED, message);
    }
}
