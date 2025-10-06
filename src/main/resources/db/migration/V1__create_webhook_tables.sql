-- Create orders table
CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY,
    merchantTransactionId VARCHAR(100) NOT NULL UNIQUE,
    reference_id UUID,
    amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    customer_name VARCHAR(255),
    customer_email VARCHAR(255),
    customer_phone VARCHAR(50),
    created_date TIMESTAMP NOT NULL,
    updated_date TIMESTAMP NOT NULL
);

-- Create indexes for orders
CREATE INDEX IF NOT EXISTS idx_order_merchant_tx_id ON orders(merchantTransactionId);
CREATE INDEX IF NOT EXISTS idx_order_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_order_reference_id ON orders(reference_id);

-- Create payment_transactions table
CREATE TABLE IF NOT EXISTS payment_transactions (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    appypayTransactionId VARCHAR(100) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    payment_method VARCHAR(50),
    reference_number VARCHAR(50),
    reference_entity VARCHAR(20),
    transaction_date TIMESTAMP NOT NULL,
    created_date TIMESTAMP NOT NULL,
    updated_date TIMESTAMP NOT NULL,
    error_message TEXT
);

-- Create indexes for payment_transactions
CREATE INDEX IF NOT EXISTS idx_payment_tx_appypay_id ON payment_transactions(appypayTransactionId);
CREATE INDEX IF NOT EXISTS idx_payment_tx_order_id ON payment_transactions(order_id);
CREATE INDEX IF NOT EXISTS idx_payment_tx_status ON payment_transactions(status);
CREATE INDEX IF NOT EXISTS idx_payment_tx_created ON payment_transactions(transaction_date);

-- Create webhook_events table
CREATE TABLE IF NOT EXISTS webhook_events (
    id UUID PRIMARY KEY,
    appypayTransactionId VARCHAR(100) NOT NULL UNIQUE,
    merchant_transaction_id VARCHAR(100),
    webhook_type VARCHAR(50) NOT NULL,
    processingStatus VARCHAR(20) NOT NULL,
    payload TEXT NOT NULL,
    received_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP,
    retry_count INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,
    created_date TIMESTAMP NOT NULL,
    updated_date TIMESTAMP NOT NULL
);

-- Create indexes for webhook_events
CREATE INDEX IF NOT EXISTS idx_webhook_appypay_tx_id ON webhook_events(appypayTransactionId);
CREATE INDEX IF NOT EXISTS idx_webhook_status ON webhook_events(processingStatus);
CREATE INDEX IF NOT EXISTS idx_webhook_received ON webhook_events(received_at);
