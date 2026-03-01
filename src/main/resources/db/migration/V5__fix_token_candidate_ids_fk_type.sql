-- =====================================================
-- Migration V5: Fix token_candidate_ids FK type
-- =====================================================
-- Purpose: Recreate token_candidate_ids with BIGINT FK to match
--          document_access_tokens.id
-- Date: 2026-02-23
-- =====================================================

DROP TABLE IF EXISTS token_candidate_ids;

-- Column names lowercase to match PostgreSQL's folding of Hibernate's default identifiers
CREATE TABLE token_candidate_ids (
    documentaccesstokenentity_id BIGINT NOT NULL,
    candidateids INTEGER,
    candidateids_order INTEGER,
    PRIMARY KEY (documentaccesstokenentity_id, candidateids_order),
    FOREIGN KEY (documentaccesstokenentity_id) REFERENCES document_access_tokens(id) ON DELETE CASCADE
);

CREATE INDEX idx_token_candidate_ids_fk ON token_candidate_ids(documentaccesstokenentity_id);
