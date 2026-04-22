package com.alec.aitraining.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable audit event record.
 * <p>
 * Append-only: the table triggers and the service layer both enforce
 * that no row may ever be updated or deleted.
 * </p>
 */
@Entity
@Table(name = "audit_events")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * Set by the server, never supplied by the caller.
     */
    @Column(name = "timestamp", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ")
    private Instant timestamp;

    /** Who initiated the action (user-id or service account). */
    @Column(name = "actor", nullable = false, updatable = false, length = 255)
    private String actor;

    /** Verb-noun action label, e.g. {@code resource.updated}, {@code user.login}. */
    @Column(name = "action", nullable = false, updatable = false, length = 255)
    private String action;

    /** Target resource, e.g. {@code project:42}, {@code invoice:777}. */
    @Column(name = "resource", updatable = false, length = 500)
    private String resource;

    /** Outcome of the action. */
    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, updatable = false, length = 50)
    private Outcome outcome;

    /** Arbitrary JSON payload with additional details. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context", updatable = false, columnDefinition = "jsonb")
    private Map<String, Object> context;

    /**
     * SHA-256 hash of the previous event in insertion order, forming a hash chain.
     * {@code null} for the very first event.
     */
    @Column(name = "previous_hash", updatable = false, length = 64)
    private String previousHash;

    /**
     * SHA-256 hex-digest of this event's canonical fields concatenated with
     * {@link #previousHash}, providing tamper evidence.
     */
    @Column(name = "event_hash", nullable = false, updatable = false, length = 64)
    private String eventHash;
}
