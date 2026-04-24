package com.alec.aitraining.dto;

import com.alec.aitraining.domain.Outcome;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;

/**
 * Query parameters for {@code GET /audit-events}.
 * All fields are optional; missing values mean "no filter".
 */
@Schema(description = "Search filter parameters for querying audit events. All fields are optional.")
public record AuditEventSearchRequest(
        @Schema(description = "Filter by actor identity", example = "user:42") String actor,
        @Schema(description = "Filter by resource identifier", example = "invoice:777") String resource,
        @Schema(description = "Filter by action name", example = "invoice.updated") String action,
        @Schema(description = "Filter by outcome", example = "SUCCESS") Outcome outcome,
        @Schema(description = "Filter events recorded at or after this timestamp (ISO-8601)", example = "2025-01-01T00:00:00Z")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @Schema(description = "Filter events recorded at or before this timestamp (ISO-8601)", example = "2025-12-31T23:59:59Z")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
) {}
