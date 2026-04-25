package com.alec.aitraining.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * An {@link AuditEvent} that has been moved to long-term archival storage after
 * the active retention window has passed.
 */
@Entity
@Table(name = "archived_audit_events")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArchivedAuditEvent {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "timestamp", nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ")
	private Instant timestamp;

	@Column(name = "actor", nullable = false, updatable = false, length = 255)
	private String actor;

	@Column(name = "action", nullable = false, updatable = false, length = 255)
	private String action;

	@Column(name = "resource", updatable = false, length = 500)
	private String resource;

	@Enumerated(EnumType.STRING)
	@Column(name = "outcome", nullable = false, updatable = false, length = 50)
	private Outcome outcome;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "context", updatable = false, columnDefinition = "jsonb")
	private Map<String, Object> context;

	@Column(name = "previous_hash", updatable = false, length = 64)
	private String previousHash;

	@Column(name = "event_hash", nullable = false, updatable = false, length = 64)
	private String eventHash;

	@Column(name = "archived_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ")
	private Instant archivedAt;

	/** Factory method: copies an {@link AuditEvent} into an archive record. */
	public static ArchivedAuditEvent from(AuditEvent e) {
		return ArchivedAuditEvent.builder()
			.id(e.getId())
			.timestamp(e.getTimestamp())
			.actor(e.getActor())
			.action(e.getAction())
			.resource(e.getResource())
			.outcome(e.getOutcome())
			.context(e.getContext())
			.previousHash(e.getPreviousHash())
			.eventHash(e.getEventHash())
			.archivedAt(Instant.now())
			.build();
	}
}
