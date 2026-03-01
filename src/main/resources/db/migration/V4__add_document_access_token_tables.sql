-- =====================================================
-- Migration V4: Add Document Access Token Tables
-- =====================================================
-- Purpose: Tables for DocumentAccessTokenService (JWT/single-use tokens)
-- Entities: DocumentAccessTokenEntity, DocumentAccessLog, DocumentAccessAudit
-- Date: 2026-02-23
-- =====================================================

-- Document access tokens (multi-use JWT tokens)
CREATE TABLE IF NOT EXISTS document_access_tokens (
    id BIGSERIAL PRIMARY KEY,
    DTYPE VARCHAR(31),
    token VARCHAR(1000) UNIQUE NOT NULL,
    employer_id VARCHAR(255) NOT NULL,
    package_type VARCHAR(255),
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    last_accessed_at TIMESTAMP,
    download_count INTEGER NOT NULL DEFAULT 0,
    max_downloads INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_document_access_tokens_token ON document_access_tokens(token);
CREATE INDEX IF NOT EXISTS idx_document_access_tokens_employer ON document_access_tokens(employer_id);
CREATE INDEX IF NOT EXISTS idx_document_access_tokens_expires ON document_access_tokens(expires_at);

-- Element collection for candidate IDs (join column: DocumentAccessTokenEntity_id)
CREATE TABLE IF NOT EXISTS token_candidate_ids (
    DocumentAccessTokenEntity_id BIGINT NOT NULL,
    candidateIds INTEGER,
    candidateIds_ORDER INTEGER,
    PRIMARY KEY (DocumentAccessTokenEntity_id, candidateIds_ORDER),
    FOREIGN KEY (DocumentAccessTokenEntity_id) REFERENCES document_access_tokens(id) ON DELETE CASCADE
);

-- Single-use document download tokens (DocumentAccessLog)
CREATE TABLE IF NOT EXISTS document_access_log (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(1000),
    employer_id VARCHAR(255),
    candidate_id INTEGER,
    document_id INTEGER,
    expires_at TIMESTAMP,
    accessed BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_document_access_log_token ON document_access_log(token);

-- Audit trail for document access (DocumentAccessAudit)
CREATE TABLE IF NOT EXISTS document_access_audit (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(1000),
    candidate_id INTEGER,
    document_id INTEGER,
    accessed_at TIMESTAMP NOT NULL,
    ip_address VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_document_access_audit_token ON document_access_audit(token);
CREATE INDEX IF NOT EXISTS idx_document_access_audit_accessed ON document_access_audit(accessed_at);
