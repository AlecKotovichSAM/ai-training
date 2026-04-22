package com.alec.aitraining.repository;

import com.alec.aitraining.domain.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID>, JpaSpecificationExecutor<AuditEvent> {

    /** Fetches the most-recently inserted event so we can build the hash chain. */
    @Query("SELECT e FROM AuditEvent e ORDER BY e.timestamp DESC LIMIT 1")
    Optional<AuditEvent> findLatest();

    /** All events older than the given cutoff — used by the retention job. */
    @Query("SELECT e FROM AuditEvent e WHERE e.timestamp < :cutoff ORDER BY e.timestamp ASC")
    Page<AuditEvent> findOlderThan(@Param("cutoff") Instant cutoff, Pageable pageable);
}
