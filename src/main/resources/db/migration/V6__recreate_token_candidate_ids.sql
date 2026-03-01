-- =====================================================
-- Migration V6: Recreate token_candidate_ids with correct column names
-- =====================================================
-- Purpose: Match entity's explicit @JoinColumn/@Column names
--          (documentaccesstokenentity_id, candidateids, candidateids_order)
-- Date: 2026-02-23
-- =====================================================

DROP TABLE IF EXISTS token_candidate_ids;

CREATE TABLE token_candidate_ids (
    documentaccesstokenentity_id BIGINT NOT NULL,
    candidateids INTEGER,
    candidateids_order INTEGER,
    PRIMARY KEY (documentaccesstokenentity_id, candidateids_order),
    FOREIGN KEY (documentaccesstokenentity_id) REFERENCES document_access_tokens(id) ON DELETE CASCADE
);

CREATE INDEX idx_token_candidate_ids_fk ON token_candidate_ids(documentaccesstokenentity_id);
