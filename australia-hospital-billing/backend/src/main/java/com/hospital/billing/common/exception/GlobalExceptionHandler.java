package com.hospital.billing.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.kafka.KafkaException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.ArrayList;
import java.util.List;

/**
 * Single place that turns exceptions into RFC 7807 problem-details responses.
 *
 * <p>Kafka note: exceptions thrown inside {@code @KafkaListener} methods never reach this
 * advice (there is no HTTP request). They are handled by the {@code DefaultErrorHandler}
 * in {@code KafkaErrorHandlingConfig} (exponential back-off, then dead-letter topic).
 * Only a failure to PUBLISH during an HTTP request surfaces here, as a 503.</p>
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ProblemDetailFactory problemDetailFactory;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleBodyValidation(MethodArgumentNotValidException ex,
                                                              HttpServletRequest request) {
        List<FieldViolation> violations = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            violations.add(new FieldViolation(error.getField(), error.getDefaultMessage()));
        }
        ex.getBindingResult().getGlobalErrors().forEach(error ->
                violations.add(new FieldViolation(error.getObjectName(), error.getDefaultMessage())));
        return build(ErrorCode.VALIDATION_FAILED,
                "Request validation failed with %d error(s)".formatted(violations.size()), violations, request);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ProblemDetail> handleParameterValidation(HandlerMethodValidationException ex,
                                                                   HttpServletRequest request) {
        List<FieldViolation> violations = ex.getAllErrors().stream()
                .map(error -> new FieldViolation(
                        error instanceof FieldError fieldError ? fieldError.getField() : "parameter",
                        error.getDefaultMessage()))
                .toList();
        return build(ErrorCode.VALIDATION_FAILED, "Request parameter validation failed", violations, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex,
                                                                   HttpServletRequest request) {
        List<FieldViolation> violations = ex.getConstraintViolations().stream()
                .map(v -> new FieldViolation(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return build(ErrorCode.VALIDATION_FAILED, "Validation failed", violations, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadable(HttpMessageNotReadableException ex,
                                                          HttpServletRequest request) {
        return build(ErrorCode.MALFORMED_REQUEST, "Request body is missing or is not valid JSON", List.of(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest request) {
        return build(ErrorCode.MALFORMED_REQUEST,
                "Parameter '%s' has an invalid value '%s'".formatted(ex.getName(), ex.getValue()), List.of(), request);
    }

    /** e.g. ?sort=unknownField */
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ProblemDetail> handleBadSort(PropertyReferenceException ex, HttpServletRequest request) {
        return build(ErrorCode.MALFORMED_REQUEST, "Unknown sort property '%s'".formatted(ex.getPropertyName()),
                List.of(), request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleMissingParameter(MissingServletRequestParameterException ex,
                                                                HttpServletRequest request) {
        return build(ErrorCode.MALFORMED_REQUEST,
                "Required parameter '%s' is missing".formatted(ex.getParameterName()), List.of(), request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                  HttpServletRequest request) {
        return build(ErrorCode.METHOD_NOT_ALLOWED, ex.getMessage(), List.of(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return build(ErrorCode.RESOURCE_NOT_FOUND, "No endpoint " + request.getRequestURI(), List.of(), request);
    }

    /** All business exceptions: the exception itself carries its error code and status. */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetail> handleDomain(DomainException ex, HttpServletRequest request) {
        log.info("Business rule rejected request: {} - {}", ex.getErrorCode(), ex.getMessage());
        return build(ex.getErrorCode(), ex.getMessage(), ex.getDetails(), request);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLock(ObjectOptimisticLockingFailureException ex,
                                                              HttpServletRequest request) {
        return build(ErrorCode.CONCURRENT_MODIFICATION,
                "The record was changed by another user. Reload and try again.", List.of(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException ex,
                                                             HttpServletRequest request) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return build(ErrorCode.DATA_INTEGRITY_VIOLATION,
                "The request conflicts with existing data (duplicate or overlapping record).", List.of(), request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException ex,
                                                              HttpServletRequest request) {
        return build(ErrorCode.AUTHENTICATION_FAILED, ex.getMessage(), List.of(), request);
    }

    /** Thrown by @PreAuthorize when the role is not allowed. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(ErrorCode.ACCESS_DENIED, "You do not have permission to perform this action", List.of(), request);
    }

    @ExceptionHandler(KafkaException.class)
    public ResponseEntity<ProblemDetail> handleKafka(KafkaException ex, HttpServletRequest request) {
        log.error("Failed to publish event to Kafka", ex);
        return build(ErrorCode.MESSAGING_UNAVAILABLE,
                "The claims messaging service is temporarily unavailable. Please retry.", List.of(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(ErrorCode.INTERNAL_ERROR,
                "An unexpected error occurred. Quote the correlationId when contacting support.", List.of(), request);
    }

    private ResponseEntity<ProblemDetail> build(ErrorCode code, String message, List<?> details,
                                                HttpServletRequest request) {
        ProblemDetail body = problemDetailFactory.create(code, message, request.getRequestURI(), details);
        return ResponseEntity.status(code.status())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body);
    }
}
