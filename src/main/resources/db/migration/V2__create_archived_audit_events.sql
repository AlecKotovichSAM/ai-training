-- ============================================================
-- V2: archived_audit_events table (retention archival target)
-- ============================================================

CREATE TABLE archived_audit_events (
    id             UUID                     NOT NULL,
    timestamp      TIMESTAMPTZ              NOT NULL,
    actor          VARCHAR(255)             NOT NULL,
    action         VARCHAR(255)             NOT NULL,
    resource       VARCHAR(500),
    outcome        VARCHAR(50)              NOT NULL,
    context        JSONB,
    previous_hash  VARCHAR(64),
    event_hash     VARCHAR(64)              NOT NULL,
    archived_at    TIMESTAMPTZ              NOT NULL DEFAULT now(),

    CONSTRAINT pk_archived_audit_events PRIMARY KEY (id)
);

CREATE INDEX idx_arch_audit_events_actor     ON archived_audit_events (actor);
CREATE INDEX idx_arch_audit_events_timestamp ON archived_audit_events (timestamp DESC);
CREATE INDEX idx_arch_audit_events_archived  ON archived_audit_events (archived_at DESC);
