package com.hospital.billing.common.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * Writes a problem-details body directly to the servlet response. Needed in filters and
 * Spring Security handlers, which run before (outside) the @RestControllerAdvice.
 */
@Component
@RequiredArgsConstructor
public class ProblemResponseWriter {

    private final ProblemDetailFactory problemDetailFactory;
    private final ObjectMapper objectMapper;

    public void write(HttpServletRequest request, HttpServletResponse response,
                      ErrorCode code, String message) throws IOException {
        ProblemDetail problem = problemDetailFactory.create(code, message, request.getRequestURI(), List.of());
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
