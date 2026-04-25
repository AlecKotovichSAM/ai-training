package com.alec.aitraining.repository;

import com.alec.aitraining.domain.ArchivedAuditEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArchivedAuditEventRepository extends JpaRepository<ArchivedAuditEvent, UUID> {
}
