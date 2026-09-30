package com.hospital.billing.common.exception;

public class EntityNotFoundException extends DomainException {

    public EntityNotFoundException(String entityName, Object id) {
        super(ErrorCode.RESOURCE_NOT_FOUND, "%s not found with id %s".formatted(entityName, id));
    }

    public EntityNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }
}
