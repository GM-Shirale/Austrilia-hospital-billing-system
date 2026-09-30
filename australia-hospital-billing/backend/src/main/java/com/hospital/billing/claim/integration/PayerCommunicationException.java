package com.hospital.billing.claim.integration;

/** Transient failure talking to a payer (timeout, 5xx): safe to retry. */
public class PayerCommunicationException extends RuntimeException {

    public PayerCommunicationException(String message) {
        super(message);
    }

    public PayerCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
