package com.hospital.billing.common.exception;

/** One itemised validation error, e.g. {"field":"medicareNo","message":"invalid check digit"}. */
public record FieldViolation(String field, String message) {
}
