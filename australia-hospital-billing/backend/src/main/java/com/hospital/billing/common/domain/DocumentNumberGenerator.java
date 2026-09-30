package com.hospital.billing.common.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Year;

/**
 * Human-readable, gap-tolerant document numbers such as BILL-2026-001042, backed by a
 * PostgreSQL sequence (safe under concurrency, unlike SELECT MAX(id) + 1).
 */
@Component
@RequiredArgsConstructor
public class DocumentNumberGenerator {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public String next(DocumentType type) {
        Long value = jdbcTemplate.queryForObject("SELECT nextval('document_number_seq')", Long.class);
        return "%s-%d-%06d".formatted(type.prefix(), Year.now(clock).getValue(), value);
    }
}
