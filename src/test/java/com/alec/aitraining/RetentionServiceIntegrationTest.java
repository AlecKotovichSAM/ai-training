package com.alec.aitraining;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.alec.aitraining.domain.AuditEvent;
import com.alec.aitraining.domain.Outcome;
import com.alec.aitraining.repository.ArchivedAuditEventRepository;
import com.alec.aitraining.repository.AuditEventRepository;
import com.alec.aitraining.service.RetentionService;

@SpringBootTest
class RetentionServiceIntegrationTest {

	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

	@Autowired
	AuditEventRepository auditRepo;

	@Autowired
	ArchivedAuditEventRepository archiveRepo;

	@Autowired
	RetentionService retentionService;

	// No cleanup in @BeforeEach - audit data should never be deleted
	// Test uses unique data to avoid conflicts

	@Test
	@DisplayName("events older than retention window are archived but preserved in hot table")
	void archivesExpiredEvents() {
		String uniqueActor = "test-user:" + System.currentTimeMillis();

		AuditEvent old = AuditEvent.builder()
			.timestamp(Instant.now().minus(101, ChronoUnit.DAYS))
			.actor(uniqueActor)
			.action("user.login")
			.outcome(Outcome.SUCCESS)
			.previousHash(null)
			.eventHash("deadbeef")
			.build();

		AuditEvent fresh = AuditEvent.builder()
			.timestamp(Instant.now().minus(1, ChronoUnit.DAYS))
			.actor(uniqueActor)
			.action("user.login")
			.outcome(Outcome.SUCCESS)
			.previousHash("deadbeef")
			.eventHash("cafebabe")
			.build();

		long initialAuditCount = auditRepo.count();
		long initialArchiveCount = archiveRepo.count();

		auditRepo.save(old);
		auditRepo.save(fresh);

		ReflectionTestUtils.setField(retentionService, "retentionDays", 90);

		retentionService.archiveExpiredEvents();

		assertThat(auditRepo.count()).isEqualTo(initialAuditCount + 2);
		assertThat(archiveRepo.count()).isEqualTo(initialArchiveCount + 1);
		assertThat(archiveRepo.findAll().stream().anyMatch(a -> a.getActor().equals(uniqueActor)))
			.isTrue();
	}

	@Test
	@DisplayName("retention job is idempotent — repeated runs do not duplicate archive entries")
	void idempotentOnRepeatedRuns() {
		String uniqueActor = "idempotent-user:" + System.currentTimeMillis();

		AuditEvent old = AuditEvent.builder()
			.timestamp(Instant.now().minus(101, ChronoUnit.DAYS))
			.actor(uniqueActor)
			.action("user.login")
			.outcome(Outcome.SUCCESS)
			.previousHash(null)
			.eventHash("idem-hash")
			.build();

		auditRepo.save(old);

		ReflectionTestUtils.setField(retentionService, "retentionDays", 90);

		retentionService.archiveExpiredEvents();
		long archiveCountAfterFirst = archiveRepo.findAll().stream().filter(a -> a.getActor().equals(uniqueActor))
			.count();

		// Second run must not throw and must not create duplicate archive records
		retentionService.archiveExpiredEvents();
		long archiveCountAfterSecond = archiveRepo.findAll().stream().filter(a -> a.getActor().equals(uniqueActor))
			.count();

		assertThat(archiveCountAfterFirst).isEqualTo(1);
		assertThat(archiveCountAfterSecond).isEqualTo(1);
	}
}
