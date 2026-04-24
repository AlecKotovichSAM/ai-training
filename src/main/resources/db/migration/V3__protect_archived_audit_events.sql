-- ============================================================
-- V3: immutability triggers for archived_audit_events
-- ============================================================

CREATE OR REPLACE FUNCTION archived_audit_events_no_update()
    RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'archived_audit_events is append-only: UPDATE is forbidden';
END;
$$;

CREATE TRIGGER trg_archived_audit_events_no_update
    BEFORE UPDATE ON archived_audit_events
    FOR EACH ROW EXECUTE FUNCTION archived_audit_events_no_update();

CREATE OR REPLACE FUNCTION archived_audit_events_no_delete()
    RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'archived_audit_events is append-only: DELETE is forbidden';
END;
$$;

CREATE TRIGGER trg_archived_audit_events_no_delete
    BEFORE DELETE ON archived_audit_events
    FOR EACH ROW EXECUTE FUNCTION archived_audit_events_no_delete();
