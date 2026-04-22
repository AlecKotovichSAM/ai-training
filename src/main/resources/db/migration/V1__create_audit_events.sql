-- ============================================================
-- V1: audit_events table (append-only, immutable)
-- ============================================================

CREATE TABLE audit_events (
    id             UUID                     NOT NULL DEFAULT gen_random_uuid(),
    timestamp      TIMESTAMPTZ              NOT NULL DEFAULT now(),
    actor          VARCHAR(255)             NOT NULL,
    action         VARCHAR(255)             NOT NULL,
    resource       VARCHAR(500),
    outcome        VARCHAR(50)              NOT NULL,
    context        JSONB,
    previous_hash  VARCHAR(64),
    event_hash     VARCHAR(64)              NOT NULL,

    CONSTRAINT pk_audit_events PRIMARY KEY (id)
);

-- Indexes for the main search use-cases
CREATE INDEX idx_audit_events_actor     ON audit_events (actor);
CREATE INDEX idx_audit_events_resource  ON audit_events (resource);
CREATE INDEX idx_audit_events_timestamp ON audit_events (timestamp DESC);
CREATE INDEX idx_audit_events_action    ON audit_events (action);

-- Trigger to enforce immutability (no UPDATE allowed)
CREATE OR REPLACE FUNCTION audit_events_no_update()
    RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'audit_events is append-only: UPDATE is forbidden';
END;
$$;

CREATE TRIGGER trg_audit_events_no_update
    BEFORE UPDATE ON audit_events
    FOR EACH ROW EXECUTE FUNCTION audit_events_no_update();

-- Trigger to enforce immutability (no DELETE allowed)
CREATE OR REPLACE FUNCTION audit_events_no_delete()
    RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'audit_events is append-only: DELETE is forbidden';
END;
$$;

CREATE TRIGGER trg_audit_events_no_delete
    BEFORE DELETE ON audit_events
    FOR EACH ROW EXECUTE FUNCTION audit_events_no_delete();
