-- POC: paywall payment initiation (per-user resource checkout)
CREATE TABLE IF NOT EXISTS billable_resources (
    resource_id   VARCHAR(255) NOT NULL,
    resource_type VARCHAR(64)  NOT NULL,
    single_buyer  BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (resource_id, resource_type)
);

COMMENT ON TABLE billable_resources IS 'Resources that may be purchased via payment initiation';

INSERT INTO billable_resources (resource_id, resource_type, single_buyer)
VALUES ('job-123', 'JOB_DOCUMENT', FALSE)
ON CONFLICT (resource_id, resource_type) DO NOTHING;

INSERT INTO billable_resources (resource_id, resource_type, single_buyer)
VALUES ('job-456', 'JOB_OFFER', TRUE)
ON CONFLICT (resource_id, resource_type) DO NOTHING;

CREATE TABLE IF NOT EXISTS payment_initiation_orders (
    id                       UUID PRIMARY KEY,
    user_id                  VARCHAR(255) NOT NULL,
    resource_id              VARCHAR(255) NOT NULL,
    resource_type            VARCHAR(64)  NOT NULL,
    merchant_transaction_id  VARCHAR(100) NOT NULL UNIQUE,
    payment_url              TEXT         NOT NULL,
    status                   VARCHAR(32)  NOT NULL,
    payment_session_expires_at TIMESTAMPTZ NOT NULL,
    created_at               TIMESTAMPTZ  NOT NULL,
    updated_at               TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_pay_init_user_resource UNIQUE (user_id, resource_id)
);

CREATE INDEX IF NOT EXISTS idx_pay_init_user_resource ON payment_initiation_orders (user_id, resource_id);
CREATE INDEX IF NOT EXISTS idx_pay_init_resource_status ON payment_initiation_orders (resource_id, status);

COMMENT ON TABLE payment_initiation_orders IS 'Checkout sessions for resource paywall (JWT user scoped)';
