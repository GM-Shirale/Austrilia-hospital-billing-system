package com.hospital.billing.common.exception;

public class InvalidStateTransitionException extends DomainException {

    public InvalidStateTransitionException(String entity, Enum<?> from, Enum<?> to) {
        super(ErrorCode.INVALID_STATE_TRANSITION,
                "%s cannot move from %s to %s".formatted(entity, from, to));
    }
}
