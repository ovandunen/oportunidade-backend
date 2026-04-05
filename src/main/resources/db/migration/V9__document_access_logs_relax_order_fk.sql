-- =============================================================================
-- Migration V9: Document access audit — optional order reference
-- =============================================================================
-- Audit rows may record access using a token whose orderId claim does not
-- match a row in orders (env mismatch, wiped DB, deleted orders), or flows
-- that have no persisted order (document_access_tokens). Foreign keys on
-- audit tables also prevent retaining history after order deletion.
-- =============================================================================

ALTER TABLE document_access_logs
    DROP CONSTRAINT IF EXISTS fk_access_log_order;

ALTER TABLE document_access_logs
    ALTER COLUMN order_id DROP NOT NULL;

COMMENT ON COLUMN document_access_logs.order_id IS
    'Optional link to orders.id when known; NULL for token-only access or unknown order.';
