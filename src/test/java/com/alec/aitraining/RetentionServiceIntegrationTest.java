package com.alec.aitraining;

import com.alec.aitraining.domain.AuditEvent;
import com.alec.aitraining.domain.Outcome;
import com.alec.aitraining.repository.ArchivedAuditEventRepository;
import com.alec.aitraining.repository.AuditEventRepository;
import com.alec.aitraining.service.RetentionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RetentionServiceIntegrationTest {

    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16-alpine");

    @Autowired AuditEventRepository        auditRepo;
    @Autowired ArchivedAuditEventRepository archiveRepo;
    @Autowired RetentionService            retentionService;

    // No cleanup in @BeforeEach - audit data should never be deleted
    // Test uses unique data to avoid conflicts

    @Test
    @DisplayName("events older than retention window are archived but preserved in hot table")
    void archivesExpiredEvents() {
        // Use unique actors to avoid conflicts with other tests
        String uniqueActor = "test-user:" + System.currentTimeMillis();
        
        // One old event (101 days ago) and one fresh event
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

        // Override retention to 90 days
        ReflectionTestUtils.setField(retentionService, "retentionDays", 90);

        retentionService.archiveExpiredEvents();

        // Audit events should never be deleted - compliance requirement
        assertThat(auditRepo.count()).isEqualTo(initialAuditCount + 2); // Both events remain
        // But old event should be copied to archive
        assertThat(archiveRepo.count()).isEqualTo(initialArchiveCount + 1); // Only old event archived
        assertThat(archiveRepo.findAll().stream()
                .anyMatch(archived -> archived.getActor().equals(uniqueActor))).isTrue();
    }
}
