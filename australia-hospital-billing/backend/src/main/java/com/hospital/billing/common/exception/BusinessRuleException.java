package com.hospital.billing.common.exception;

public class BusinessRuleException extends DomainException {

    public BusinessRuleException(String message) {
        super(ErrorCode.BUSINESS_RULE_VIOLATION, message);
    }
}
