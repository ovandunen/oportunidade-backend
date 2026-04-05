-- =============================================================================
-- Migration V8: Align employer_references ID sequence with Hibernate
-- =============================================================================
-- V2 used BIGSERIAL for employer_references.id, which creates sequence
-- employer_references_id_seq. PanacheEntity / Hibernate expects
-- nextval('employer_references_SEQ') → catalog name employer_references_seq.
-- Rename the serial sequence so inserts resolve correctly.
-- =============================================================================

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_class c
        JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE c.relkind = 'S'
          AND c.relname = 'employer_references_id_seq'
          AND n.nspname = current_schema()
    )
       AND NOT EXISTS (
        SELECT 1
        FROM pg_class c
        JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE c.relkind = 'S'
          AND c.relname = 'employer_references_seq'
          AND n.nspname = current_schema()
    ) THEN
        ALTER SEQUENCE employer_references_id_seq RENAME TO employer_references_seq;
    END IF;
END $$;
