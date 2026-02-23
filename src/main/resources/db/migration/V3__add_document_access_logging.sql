-- =====================================================
-- Migration V3: Add Document Access Logging
-- =====================================================
-- Purpose: Track all document access attempts for audit and compliance
-- Phase: Phase 1 - Task 5
-- Date: 2026-02-18
-- =====================================================

-- Create document access logs table
CREATE TABLE document_access_logs (
    id BIGSERIAL PRIMARY KEY,
    order_id UUID NOT NULL,
    employer_id VARCHAR(255) NOT NULL,
    employer_email VARCHAR(255),
    token_used VARCHAR(2000) NOT NULL,
    token_type VARCHAR(20) NOT NULL CHECK (token_type IN ('MULTI_USE', 'SINGLE_USE')),
    candidate_id VARCHAR(255),
    document_id VARCHAR(255),
    document_name VARCHAR(500),
    ip_address VARCHAR(45),
    user_agent VARCHAR(1000),
    accessed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    success BOOLEAN NOT NULL DEFAULT true,
    error_message VARCHAR(1000),
    http_status_code INTEGER,
    document_size_bytes BIGINT
);

-- Create indexes for efficient querying
CREATE INDEX idx_access_log_token ON document_access_logs(token_used);
CREATE INDEX idx_access_log_order ON document_access_logs(order_id);
CREATE INDEX idx_access_log_employer ON document_access_logs(employer_id);
CREATE INDEX idx_access_log_timestamp ON document_access_logs(accessed_at);
CREATE INDEX idx_access_log_ip ON document_access_logs(ip_address);
CREATE INDEX idx_access_log_success ON document_access_logs(success);
CREATE INDEX idx_access_log_candidate ON document_access_logs(candidate_id);
CREATE INDEX idx_access_log_document ON document_access_logs(document_id);

-- Add comment to table
COMMENT ON TABLE document_access_logs IS 
'Audit trail of all document access attempts. Records both successful and failed access for compliance and security monitoring.';

-- Add comments to key columns
COMMENT ON COLUMN document_access_logs.token_used IS 
'JWT or single-use token used for access. Stored for audit purposes.';

COMMENT ON COLUMN document_access_logs.token_type IS 
'Type of token: MULTI_USE (JWT) or SINGLE_USE (one-time download link)';

COMMENT ON COLUMN document_access_logs.success IS 
'Whether the access attempt was successful. Failed attempts help detect suspicious activity.';

COMMENT ON COLUMN document_access_logs.ip_address IS 
'IP address of the accessor. Supports both IPv4 (15 chars) and IPv6 (45 chars max).';

COMMENT ON COLUMN document_access_logs.accessed_at IS 
'Timestamp when access was attempted. Indexed for efficient time-based queries.';

-- =====================================================
-- Data Integrity: Add foreign key to orders table
-- =====================================================
ALTER TABLE document_access_logs 
    ADD CONSTRAINT fk_access_log_order 
    FOREIGN KEY (order_id) 
    REFERENCES orders(id) 
    ON DELETE CASCADE;

-- =====================================================
-- Performance: Create composite index for common queries
-- =====================================================
-- Query pattern: "Show me all successful downloads for an order"
CREATE INDEX idx_access_log_order_success 
    ON document_access_logs(order_id, success, accessed_at DESC);

-- Query pattern: "Show me all access by employer in date range"
CREATE INDEX idx_access_log_employer_date 
    ON document_access_logs(employer_id, accessed_at DESC);

-- Query pattern: "Detect suspicious activity from same IP"
CREATE INDEX idx_access_log_ip_date 
    ON document_access_logs(ip_address, accessed_at DESC);

-- =====================================================
-- Future: Partitioning setup (commented out for Phase 1)
-- =====================================================
-- For high-volume environments, consider partitioning by date:
-- 
-- CREATE TABLE document_access_logs_2026_02 PARTITION OF document_access_logs
--     FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');
-- 
-- CREATE TABLE document_access_logs_2026_03 PARTITION OF document_access_logs
--     FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');
