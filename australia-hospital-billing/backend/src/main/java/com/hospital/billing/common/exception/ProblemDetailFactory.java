package com.hospital.billing.common.exception;

import com.hospital.billing.common.web.CorrelationIdFilter;
import org.slf4j.MDC;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Builds RFC 7807 / RFC 9457 "problem details" bodies with a fixed extension schema:
 * <pre>
 * {
 *   "type": "https://api.hospital-billing.example/errors/resource-not-found",
 *   "title": "Resource not found",
 *   "status": 404,
 *   "detail": "Patient not found with id 42",
 *   "instance": "/api/v1/patients/42",
 *   "timestamp": "2026-09-24T10:15:30Z",
 *   "errorCode": "RESOURCE_NOT_FOUND",
 *   "message": "Patient not found with id 42",
 *   "correlationId": "5f0c...",
 *   "details": []
 * }
 * </pre>
 * Used by the controller advice AND by servlet filters / security handlers, so every
 * error in the system looks the same.
 */
@Component
public class ProblemDetailFactory {

    private static final String TYPE_BASE = "https://api.hospital-billing.example/errors/";

    public ProblemDetail create(ErrorCode code, String message, String path, List<?> details) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(code.status(), message);
        problem.setTitle(code.title());
        problem.setType(URI.create(TYPE_BASE + code.name().toLowerCase(Locale.ROOT).replace('_', '-')));
        if (path != null) {
            problem.setInstance(URI.create(path));
        }
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", code.name());
        problem.setProperty("message", message);
        problem.setProperty("correlationId", MDC.get(CorrelationIdFilter.MDC_KEY));
        problem.setProperty("details", details == null ? List.of() : details);
        return problem;
    }
}
