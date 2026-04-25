package com.alec.aitraining.dto;

import java.util.Map;

import com.alec.aitraining.domain.Outcome;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Inbound payload for {@code POST /audit-events}.
 *
 * <p>
 * {@code timestamp} is intentionally absent — it is always set by the server.
 */
@Schema(description = "Payload for creating a new audit event")
public record CreateAuditEventRequest(
		@Schema(description = "Identity of the user or service that performed the action", example = "user:42") @NotBlank(message = "actor is required") String actor,
		@Schema(description = "Name of the action that was performed", example = "invoice.updated") @NotBlank(message = "action is required") String action,
		@Schema(description = "Resource that was affected by the action", example = "invoice:777") String resource,
		@Schema(description = "Outcome of the action", example = "SUCCESS") @NotNull(message = "outcome is required") Outcome outcome,
		@Schema(description = "Additional contextual key-value pairs", example = "{\"ip\": \"10.0.0.1\"}") Map<String, Object> context) {
}
