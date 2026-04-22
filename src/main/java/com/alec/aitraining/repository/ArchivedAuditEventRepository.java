package com.alec.aitraining.repository;

import com.alec.aitraining.domain.ArchivedAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ArchivedAuditEventRepository extends JpaRepository<ArchivedAuditEvent, UUID> {
}
