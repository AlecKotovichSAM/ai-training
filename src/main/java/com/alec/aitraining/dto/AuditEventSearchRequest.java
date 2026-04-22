package com.alec.aitraining.dto;

import com.alec.aitraining.domain.Outcome;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;

/**
 * Query parameters for {@code GET /audit-events}.
 * All fields are optional; missing values mean "no filter".
 */
public record AuditEventSearchRequest(
        String actor,
        String resource,
        String action,
        Outcome outcome,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
) {}
