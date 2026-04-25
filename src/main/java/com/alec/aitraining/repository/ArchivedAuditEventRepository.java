package com.alec.aitraining.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.alec.aitraining.domain.ArchivedAuditEvent;

public interface ArchivedAuditEventRepository extends JpaRepository<ArchivedAuditEvent, UUID> {

}
