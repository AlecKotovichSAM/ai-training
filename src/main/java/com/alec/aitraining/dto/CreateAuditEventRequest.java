package com.alec.aitraining.dto;

import com.alec.aitraining.domain.Outcome;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Inbound payload for {@code POST /audit-events}.
 * <p>
 * {@code timestamp} is intentionally absent — it is always set by the server.
 * </p>
 */
public record CreateAuditEventRequest(

        @NotBlank(message = "actor is required")
        String actor,

        @NotBlank(message = "action is required")
        String action,

        String resource,

        @NotNull(message = "outcome is required")
        Outcome outcome,

        Map<String, Object> context
) {}
