-- =============================================================================
-- Migration V7: Payment reference + catalog reference tables
-- =============================================================================
-- Purpose: Persist JPA entities PaymentReferenceEntity, ReferenceEntity, and
--          the reference_amounts element collection (AmountDomainValue).
--          Aligns schema with Hibernate mappings (snake_case columns).
-- =============================================================================

CREATE TABLE payment_reference (
    id UUID NOT NULL PRIMARY KEY,
    reference_number VARCHAR(255),
    entity_code VARCHAR(255),
    reference_text VARCHAR(255),
    amount NUMERIC(19, 2),
    currency VARCHAR(255),
    due_date TIMESTAMP WITH TIME ZONE,
    merchant_transaction_id VARCHAR(255)
);

CREATE INDEX idx_payment_reference_merchant_tx ON payment_reference (merchant_transaction_id);

CREATE TABLE reference (
    id UUID NOT NULL PRIMARY KEY,
    entity VARCHAR(255),
    reference_number VARCHAR(255),
    currency VARCHAR(255),
    min_amount DOUBLE PRECISION,
    max_amount DOUBLE PRECISION,
    start_date TIMESTAMP WITH TIME ZONE,
    expiration_date TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    created_date TIMESTAMP WITH TIME ZONE,
    updated_date TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_reference_reference_number ON reference (reference_number);

CREATE TABLE reference_amounts (
    reference_id UUID NOT NULL,
    amount DOUBLE PRECISION,
    description_line_1 VARCHAR(255),
    description_line_2 VARCHAR(255),
    CONSTRAINT fk_reference_amounts_reference FOREIGN KEY (reference_id) REFERENCES reference (id) ON DELETE CASCADE
);

COMMENT ON TABLE payment_reference IS 'Issued payment references (e.g. MultiCaixa) linked to merchant transactions';
COMMENT ON TABLE reference IS 'Reference catalog for payment / entity configuration';
COMMENT ON TABLE reference_amounts IS 'Allowed amount lines per reference (element collection)';
