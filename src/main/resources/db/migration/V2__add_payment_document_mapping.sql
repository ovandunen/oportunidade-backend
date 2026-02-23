-- =============================================================================
-- Migration V2: Add Payment-to-Document Mapping
-- =============================================================================
-- Purpose: Enable linking payments to specific employers and their candidate
--          documents for token-based access control
-- 
-- Changes:
-- 1. Create employer_references table for reference code lookup
-- 2. Add new columns to orders table
-- 3. Create order_candidates junction table
-- 4. Create order_odoo_documents junction table
-- 5. Add indexes for performance
--
-- Author: Oportunidade Team
-- Date: 2026-02-16
-- Phase: 1 - Critical Fixes
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Employer Reference Lookup Table
-- -----------------------------------------------------------------------------
-- Maps payment reference codes to employer identities
CREATE TABLE IF NOT EXISTS employer_references (
    id BIGSERIAL PRIMARY KEY,
    reference_code VARCHAR(255) UNIQUE NOT NULL,
    employer_id VARCHAR(255) NOT NULL,
    employer_email VARCHAR(255),
    company_name VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

COMMENT ON TABLE employer_references IS 'Maps payment reference codes from AppyPay to internal employer IDs';
COMMENT ON COLUMN employer_references.reference_code IS 'Unique reference code from payment webhook';
COMMENT ON COLUMN employer_references.employer_id IS 'Internal employer identifier';
COMMENT ON COLUMN employer_references.is_active IS 'Whether this reference can be used for new payments';

-- Indexes for fast lookups
CREATE INDEX IF NOT EXISTS idx_employer_ref_code ON employer_references(reference_code);
CREATE INDEX IF NOT EXISTS idx_employer_id ON employer_references(employer_id);
CREATE INDEX IF NOT EXISTS idx_employer_active ON employer_references(is_active);

-- -----------------------------------------------------------------------------
-- 2. Add New Columns to Orders Table
-- -----------------------------------------------------------------------------
-- Extend orders table to support document access mapping
ALTER TABLE orders ADD COLUMN IF NOT EXISTS employer_id VARCHAR(255);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS employer_email VARCHAR(255);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS package_type VARCHAR(50);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS reference_code VARCHAR(255);

COMMENT ON COLUMN orders.employer_id IS 'Internal employer ID (from employer_references)';
COMMENT ON COLUMN orders.employer_email IS 'Employer email for notifications';
COMMENT ON COLUMN orders.package_type IS 'Package type: BASIC, STANDARD, PREMIUM, ENTERPRISE';
COMMENT ON COLUMN orders.reference_code IS 'Payment reference code from AppyPay webhook';

-- Indexes for queries
CREATE INDEX IF NOT EXISTS idx_orders_employer ON orders(employer_id);
CREATE INDEX IF NOT EXISTS idx_orders_reference_code ON orders(reference_code);
CREATE INDEX IF NOT EXISTS idx_orders_package_type ON orders(package_type);

-- -----------------------------------------------------------------------------
-- 3. Order-Candidate Mapping (Many-to-Many)
-- -----------------------------------------------------------------------------
-- Links orders to specific candidate IDs (hr.applicant in Odoo)
CREATE TABLE IF NOT EXISTS order_candidates (
    order_id UUID NOT NULL,
    candidate_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (order_id, candidate_id),
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

COMMENT ON TABLE order_candidates IS 'Maps orders to specific candidate IDs for document access';
COMMENT ON COLUMN order_candidates.order_id IS 'Reference to orders table';
COMMENT ON COLUMN order_candidates.candidate_id IS 'Candidate ID (maps to hr.applicant in Odoo)';

-- Index for fast candidate lookups
CREATE INDEX IF NOT EXISTS idx_order_candidates_order ON order_candidates(order_id);
CREATE INDEX IF NOT EXISTS idx_order_candidates_candidate ON order_candidates(candidate_id);

-- -----------------------------------------------------------------------------
-- 4. Order-Document Mapping (Many-to-Many)
-- -----------------------------------------------------------------------------
-- Links orders to specific Odoo document IDs (ir.attachment)
CREATE TABLE IF NOT EXISTS order_odoo_documents (
    order_id UUID NOT NULL,
    odoo_document_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (order_id, odoo_document_id),
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

COMMENT ON TABLE order_odoo_documents IS 'Maps orders to Odoo document IDs (ir.attachment) for access control';
COMMENT ON COLUMN order_odoo_documents.order_id IS 'Reference to orders table';
COMMENT ON COLUMN order_odoo_documents.odoo_document_id IS 'Odoo document ID (ir.attachment.id)';

-- Index for fast document lookups
CREATE INDEX IF NOT EXISTS idx_order_documents_order ON order_odoo_documents(order_id);
CREATE INDEX IF NOT EXISTS idx_order_documents_doc ON order_odoo_documents(odoo_document_id);

-- -----------------------------------------------------------------------------
-- 5. Sample Data (for development/testing)
-- -----------------------------------------------------------------------------
-- Insert test employer references
-- UNCOMMENT FOR DEVELOPMENT ENVIRONMENTS ONLY
-- INSERT INTO employer_references (reference_code, employer_id, employer_email, company_name, created_at, is_active)
-- VALUES 
--     ('TEST-REF-001', 'EMP-001', 'employer1@example.com', 'Test Company A', NOW(), true),
--     ('TEST-REF-002', 'EMP-002', 'employer2@example.com', 'Test Company B', NOW(), true),
--     ('TEST-REF-003', 'EMP-003', 'employer3@example.com', 'Test Company C', NOW(), true)
-- ON CONFLICT (reference_code) DO NOTHING;

-- =============================================================================
-- Migration Complete
-- =============================================================================
