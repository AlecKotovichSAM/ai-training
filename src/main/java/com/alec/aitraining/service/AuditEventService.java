package com.alec.aitraining.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alec.aitraining.domain.AuditEvent;
import com.alec.aitraining.dto.AuditEventResponse;
import com.alec.aitraining.dto.AuditEventSearchRequest;
import com.alec.aitraining.dto.CreateAuditEventRequest;
import com.alec.aitraining.repository.AuditEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditEventService {

	private final AuditEventRepository repository;

	/**
	 * Appends a new immutable audit event.
	 *
	 * <p>
	 * The method is synchronised on the instance to guarantee that the
	 * {@code previousHash} pointer and the insertion order are consistent even
	 * under concurrent requests. For a multi-instance deployment the lock should be
	 * moved to a distributed lock (e.g. Postgres advisory lock).
	 */
	@Transactional
	public synchronized AuditEventResponse append(CreateAuditEventRequest req) {
		Instant now = Instant.now();

		String previousHash = repository.findLatest().map(AuditEvent::getEventHash).orElse(null);

		String eventHash = computeHash(req, now, previousHash);

		AuditEvent event = AuditEvent.builder()
			.timestamp(now)
			.actor(req.actor())
			.action(req.action())
			.resource(req.resource())
			.outcome(req.outcome())
			.context(req.context())
			.previousHash(previousHash)
			.eventHash(eventHash)
			.build();

		AuditEvent saved = repository.save(event);
		log.debug("Audit event appended id={} actor={} action={}", saved.getId(), saved.getActor(), saved.getAction());
		return AuditEventResponse.from(saved);
	}

	@Transactional(readOnly = true)
	public Page<AuditEventResponse> search(AuditEventSearchRequest req, Pageable pageable) {
		Specification<AuditEvent> spec = (root, query, cb) -> cb.conjunction();
		if (req.actor() != null)
			spec = spec.and((r, q, cb) -> cb.equal(r.get("actor"), req.actor()));
		if (req.resource() != null)
			spec = spec.and((r, q, cb) -> cb.equal(r.get("resource"), req.resource()));
		if (req.action() != null)
			spec = spec.and((r, q, cb) -> cb.equal(r.get("action"), req.action()));
		if (req.outcome() != null)
			spec = spec.and((r, q, cb) -> cb.equal(r.get("outcome"), req.outcome()));
		if (req.from() != null)
			spec = spec.and((r, q, cb) -> cb.greaterThanOrEqualTo(r.get("timestamp"), req.from()));
		if (req.to() != null)
			spec = spec.and((r, q, cb) -> cb.lessThanOrEqualTo(r.get("timestamp"), req.to()));
		return repository.findAll(spec, pageable).map(AuditEventResponse::from);
	}

	// ------------------------------------------------------------------
	// Hash chain helpers
	// ------------------------------------------------------------------

	/**
	 * Computes SHA-256 over the canonical string representation of the event.
	 *
	 * <p>
	 * Format: {@code timestamp|actor|action|resource|outcome|previousHash} where
	 * missing fields are represented as the empty string.
	 */
	private String computeHash(CreateAuditEventRequest req, Instant timestamp, String previousHash) {
		String canonical = String.join("|", timestamp.toString(), req.actor(), req.action(), req.resource() != null
				? req.resource()
				: "", req.outcome() != null ? req.outcome().name() : "", previousHash != null ? previousHash : "");
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 not available", e);
		}
	}
}
