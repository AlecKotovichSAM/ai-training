package com.alec.aitraining.dto;

import com.alec.aitraining.domain.AuditEvent;
import com.alec.aitraining.domain.Outcome;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Read model returned to callers — includes the hash-chain fields for
 * tamper-evidence verification.
 */
public record AuditEventResponse(
        UUID id,
        Instant timestamp,
        String actor,
        String action,
        String resource,
        Outcome outcome,
        Map<String, Object> context,
        String previousHash,
        String eventHash
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
