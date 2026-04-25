package com.alec.aitraining.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.alec.aitraining.domain.AuditEvent;

public interface AuditEventRepository
		extends JpaRepository<AuditEvent, UUID>, JpaSpecificationExecutor<AuditEvent> {

	/** Fetches the most-recently inserted event so we can build the hash chain. */
	@Query("SELECT e FROM AuditEvent e ORDER BY e.timestamp DESC LIMIT 1")
	Optional<AuditEvent> findLatest();

	/** Events older than cutoff that have not yet been copied to the archive. */
	@Query("""
			SELECT e FROM AuditEvent e
			WHERE e.timestamp < :cutoff
			  AND NOT EXISTS (SELECT 1 FROM ArchivedAuditEvent a WHERE a.id = e.id)
			ORDER BY e.timestamp ASC
			""")
	Page<AuditEvent> findOlderThan(@Param("cutoff") Instant cutoff, Pageable pageable);

}
