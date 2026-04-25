package com.alec.aitraining.service;

import com.alec.aitraining.domain.ArchivedAuditEvent;
import com.alec.aitraining.domain.AuditEvent;
import com.alec.aitraining.repository.ArchivedAuditEventRepository;
import com.alec.aitraining.repository.AuditEventRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Periodically copies audit events that have exceeded the active retention
 * window into the {@code
 * archived_audit_events} table.
 *
 * <p>
 * Events are never deleted from the hot table — the append-only invariant and
 * compliance requirements forbid any DELETE. Archival is purely for
 * query-performance offloading.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetentionService {

	private static final int BATCH_SIZE = 500;

	private final AuditEventRepository auditEventRepository;
	private final ArchivedAuditEventRepository archivedRepository;

	@Value("${audit.retention.days:90}")
	private int retentionDays;

	/**
	 * Runs every day at 02:00 UTC by default (configurable via
	 * {@code audit.retention.cron}).
	 */
	@Scheduled(cron = "${audit.retention.cron:0 0 2 * * *}")
	@Transactional
	public void archiveExpiredEvents() {
		Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
		log.info("Retention job started — archiving events older than {} (cutoff={})", retentionDays + " days", cutoff);

		int totalArchived = 0;
		Page<AuditEvent> page;
		int pageNum = 0;

		do {
			page = auditEventRepository.findOlderThan(cutoff, PageRequest.of(pageNum, BATCH_SIZE));
			List<AuditEvent> batch = page.getContent();
			if (batch.isEmpty())
				break;

			List<ArchivedAuditEvent> archives = batch.stream().map(ArchivedAuditEvent::from).toList();
			archivedRepository.saveAll(archives);

			// Do NOT delete from audit_events - this would violate audit trail compliance
			// Audit data must be preserved for investigations and regulatory requirements
			// Only copy to archived_audit_events for performance optimization

			totalArchived += batch.size();
			pageNum++;
		} while (page.hasNext());

		log.info("Retention job finished — {} events archived.", totalArchived);
	}
}
