package com.alec.aitraining.dto;

import com.alec.aitraining.domain.AuditEvent;
import com.alec.aitraining.domain.Outcome;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Read model returned to callers — includes the hash-chain fields for
 * tamper-evidence verification.
 */
@Schema(description = "Audit event response including hash-chain fields for tamper-evidence verification")
public record AuditEventResponse(
        @Schema(description = "Unique identifier of the audit event") UUID id,
        @Schema(description = "Server-assigned timestamp of when the event was recorded") Instant timestamp,
        @Schema(description = "Identity of the user or service that performed the action", example = "user:42") String actor,
        @Schema(description = "Name of the action that was performed", example = "invoice.updated") String action,
        @Schema(description = "Resource that was affected", example = "invoice:777") String resource,
        @Schema(description = "Outcome of the action", example = "SUCCESS") Outcome outcome,
        @Schema(description = "Additional contextual key-value pairs") Map<String, Object> context,
        @Schema(description = "Hash of the previous audit event in the chain (null for the first event)") String previousHash,
        @Schema(description = "SHA-256 hash of this event's content, used for tamper-evidence verification") String eventHash
) {
    public static AuditEventResponse from(AuditEvent e) {
        return new AuditEventResponse(
                e.getId(),
                e.getTimestamp(),
                e.getActor(),
                e.getAction(),
                e.getResource(),
                e.getOutcome(),
                e.getContext(),
                e.getPreviousHash(),
                e.getEventHash()
        );
    }
}
