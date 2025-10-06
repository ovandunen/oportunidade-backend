# AppyPay Webhook Integration - Oportunidade Backend

Complete Quarkus-based implementation for receiving and processing AppyPay payment webhooks.

## Overview

This integration handles payment reference notifications when customers pay at MultiCaixa ATMs in Angola through the AppyPay payment gateway.

## Features

- ✅ RESTful webhook endpoint at `/webhooks/appypay`
- ✅ Asynchronous processing with SmallRye Reactive Messaging
- ✅ Idempotency checking to prevent duplicate processing
- ✅ Status-based payment routing (Success, Pending, Failed, Cancelled)
- ✅ Integration with existing Reference management system
- ✅ Comprehensive error handling and retry logic
- ✅ Health checks and Prometheus metrics
- ✅ Database migrations with Flyway
- ✅ Full test coverage with unit and integration tests

## Technical Stack

- **Framework**: Quarkus 3.8.6
- **Language**: Java 17
- **Database**: PostgreSQL
- **ORM**: Hibernate with Panache
- **Async**: SmallRye Reactive Messaging
- **Testing**: JUnit 5, RestAssured, Mockito, AssertJ
- **Build Tool**: Gradle
- **Utilities**: Lombok, MapStruct

## Architecture

```
┌─────────────┐
│   AppyPay   │
│   System    │
└──────┬──────┘
       │ POST /webhooks/appypay
       ▼
┌──────────────────────────────┐
│  AppyPayWebhookResource      │
│  - Validates payload          │
│  - Checks idempotency         │
│  - Returns 200 immediately    │
└──────┬───────────────────────┘
       │ Queue for async processing
       ▼
┌──────────────────────────────┐
│  WebhookProcessor            │
│  - Processes asynchronously   │
└──────┬───────────────────────┘
       │
       ▼
┌──────────────────────────────┐
│  PaymentService              │
│  - Routes by status           │
│  - Creates/updates orders     │
│  - Links to references        │
│  - Creates transactions       │
└──────┬───────────────────────┘
       │
       ▼
┌──────────────────────────────┐
│  Database (PostgreSQL)       │
│  - orders                     │
│  - payment_transactions       │
│  - webhook_events             │
└──────────────────────────────┘
```

## Database Schema

### Orders Table
Tracks payment orders linked to references.

| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| merchantTransactionId | VARCHAR(100) | Unique order identifier |
| reference_id | UUID | Link to reference entity |
| amount | DECIMAL(19,2) | Order amount |
| currency | VARCHAR(3) | Currency code (AOA) |
| status | VARCHAR(20) | Order status |
| customer_name | VARCHAR(255) | Customer name |
| customer_email | VARCHAR(255) | Customer email |
| customer_phone | VARCHAR(50) | Customer phone |
| created_date | TIMESTAMP | Creation timestamp |
| updated_date | TIMESTAMP | Last update timestamp |

### Payment Transactions Table
Audit trail for all payment activities.

| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| order_id | UUID | Link to order |
| appypayTransactionId | VARCHAR(100) | AppyPay transaction ID |
| amount | DECIMAL(19,2) | Transaction amount |
| currency | VARCHAR(3) | Currency code |
| status | VARCHAR(20) | Transaction status |
| payment_method | VARCHAR(50) | Payment method |
| reference_number | VARCHAR(50) | Payment reference number |
| reference_entity | VARCHAR(20) | Reference entity code |
| transaction_date | TIMESTAMP | Transaction timestamp |
| error_message | TEXT | Error message if failed |

### Webhook Events Table
Tracks webhook events for idempotency and debugging.

| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| appypayTransactionId | VARCHAR(100) | Unique AppyPay transaction ID |
| merchant_transaction_id | VARCHAR(100) | Merchant transaction ID |
| webhook_type | VARCHAR(50) | Webhook type |
| processingStatus | VARCHAR(20) | Processing status |
| payload | TEXT | Full JSON payload |
| received_at | TIMESTAMP | Receipt timestamp |
| processed_at | TIMESTAMP | Processing timestamp |
| retry_count | INTEGER | Number of retries |
| error_message | TEXT | Error message if failed |

## API Endpoints

### POST /webhooks/appypay

Receives payment notifications from AppyPay.

**Request Body:**
```json
{
  "id": "57af18f2-64b7-4464-a503-3baf741c9f0d",
  "merchantTransactionId": "ORDER-12345",
  "type": "Charge",
  "amount": 1500.00,
  "currency": "AOA",
  "status": "Success",
  "paymentMethod": "REF",
  "reference": {
    "referenceNumber": "123456789",
    "entity": "00123",
    "dueDate": "2025-01-15T15:00:00"
  },
  "customer": {
    "name": "John Doe",
    "email": "john@example.com",
    "phone": "+244900000000"
  },
  "createdDate": "2025-01-10T11:47:13.521Z",
  "updatedDate": "2025-01-10T11:47:52.599Z"
}
```

**Response:**
```json
{
  "status": "received",
  "message": "Webhook received and queued for processing",
  "eventId": "event-uuid"
}
```

**Status Codes:**
- `200 OK` - Webhook received successfully
- `500 Internal Server Error` - Processing error

### GET /webhooks/appypay/health

Health check endpoint for the webhook service.

**Response:**
```json
{
  "status": "UP",
  "service": "AppyPay Webhook"
}
```

## Configuration

### Environment Variables

Configure the following environment variables:

```bash
# Database Configuration
export DB_URL=jdbc:postgresql://localhost:5432/oportunidade
export DB_USERNAME=postgres
export DB_PASSWORD=your_password

# Webhook Configuration (optional)
export WEBHOOK_MAX_RETRY=3
export WEBHOOK_RETRY_DELAY=60
export WEBHOOK_SECRET=your_secret_key
```

### application.properties

Key configuration properties:

```properties
# Database
quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.url=${DB_URL:jdbc:postgresql://localhost:5432/oportunidade}
quarkus.datasource.username=${DB_USERNAME:postgres}
quarkus.datasource.password=${DB_PASSWORD:postgres}

# Flyway Migrations
quarkus.flyway.migrate-at-start=true
quarkus.flyway.baseline-on-migrate=true

# Reactive Messaging
mp.messaging.outgoing.webhook-events.connector=smallrye-in-memory
mp.messaging.incoming.webhook-events.connector=smallrye-in-memory

# Health & Metrics
quarkus.smallrye-health.ui.enable=true
quarkus.micrometer.export.prometheus.enabled=true
```

## Setup Instructions

### Prerequisites

- Java 17 or higher
- PostgreSQL 12 or higher
- Gradle 7.x or higher

### 1. Clone the Repository

```bash
git clone https://github.com/ovandunen/oportunidade-backend.git
cd oportunidade-backend
```

### 2. Configure Database

Create a PostgreSQL database:

```sql
CREATE DATABASE oportunidade;
CREATE USER oportunidade_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE oportunidade TO oportunidade_user;
```

Set environment variables:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/oportunidade
export DB_USERNAME=oportunidade_user
export DB_PASSWORD=your_password
```

### 3. Build the Application

```bash
./gradlew clean build
```

### 4. Run Database Migrations

Migrations run automatically on startup. To run manually:

```bash
./gradlew quarkusDev
```

### 5. Run the Application

Development mode (with live reload):

```bash
./gradlew quarkusDev
```

Production mode:

```bash
java -jar build/quarkus-app/quarkus-run.jar
```

## Testing

### Run All Tests

```bash
./gradlew test
```

### Run Unit Tests Only

```bash
./gradlew test --tests "*.Test"
```

### Run Integration Tests Only

```bash
./gradlew test --tests "*IT"
```

### Test Coverage

Generate test coverage report:

```bash
./gradlew jacocoTestReport
```

Report available at: `build/reports/jacoco/test/html/index.html`

## Monitoring

### Health Checks

Check application health:

```bash
curl http://localhost:8080/q/health
```

View health UI:

```
http://localhost:8080/q/health-ui/
```

### Metrics

Prometheus metrics endpoint:

```bash
curl http://localhost:8080/q/metrics
```

### API Documentation

OpenAPI/Swagger UI:

```
http://localhost:8080/q/swagger-ui/
```

OpenAPI spec:

```
http://localhost:8080/q/openapi
```

## Payment Status Flow

### Success Flow
1. Webhook received with `status: "Success"`
2. Order created/updated with `PAID` status
3. Payment transaction recorded with `SUCCESS` status
4. Linked to existing reference if available

### Pending Flow
1. Webhook received with `status: "Pending"`
2. Order created with `PENDING` status
3. Payment transaction recorded with `PENDING` status
4. Awaiting payment confirmation

### Failed Flow
1. Webhook received with `status: "Failed"`
2. Order created/updated with `FAILED` status
3. Payment transaction recorded with `FAILED` status
4. Error message stored for investigation

### Cancelled Flow
1. Webhook received with `status: "Cancelled"`
2. Existing order updated to `CANCELLED` status
3. Payment transaction recorded with `CANCELLED` status

## Idempotency

The system ensures idempotency by:

1. Checking `webhook_events` table for existing transaction ID
2. Returning early if webhook already processed or in progress
3. Storing complete payload for debugging
4. Allowing retry for failed webhooks

## Error Handling

### Retry Logic

Failed webhooks are:
- Marked with `FAILED` status
- Retry count incremented
- Eligible for automatic retry (up to 3 times)
- Moved to dead letter queue after max retries

### Dead Letter Queue

Webhooks that fail after maximum retries:
- Marked with `DEAD_LETTER` status
- Stored for manual investigation
- Queryable via repository methods

### Logging

Comprehensive logging at each stage:
- Webhook receipt
- Idempotency check
- Async processing start/end
- Payment processing
- Database operations
- Error conditions

## Troubleshooting

### Issue: Webhooks not processing

**Check:**
1. Application logs: `tail -f logs/application.log`
2. Webhook events table: Query `webhook_events` for status
3. Reactive messaging: Verify in-memory connector is working

**Solution:**
- Restart application
- Check async processing errors in logs

### Issue: Database connection failed

**Check:**
1. PostgreSQL is running: `systemctl status postgresql`
2. Credentials are correct
3. Database exists

**Solution:**
```bash
export DB_URL=jdbc:postgresql://localhost:5432/oportunidade
export DB_USERNAME=postgres
export DB_PASSWORD=your_password
./gradlew quarkusDev
```

### Issue: Duplicate payments

**Check:**
1. Idempotency logic in `WebhookEventService`
2. Unique constraint on `webhook_events.appypayTransactionId`

**Solution:**
- Already handled by idempotency checks
- Verify database constraints exist

## Development Guidelines

### Adding New Payment Status

1. Add enum value to `Order.OrderStatus` or `PaymentTransaction.TransactionStatus`
2. Add handler method in `PaymentService`
3. Update `processWebhook()` switch statement
4. Add unit and integration tests

### Adding New Webhook Fields

1. Update DTOs in `ao.co.oportunidade.webhook.dto` package
2. Add Jackson annotations for JSON mapping
3. Update service logic if needed
4. Add tests

### Database Schema Changes

1. Create new Flyway migration: `V2__description.sql`
2. Place in `src/main/resources/db/migration/`
3. Restart application to apply
4. Update entities and repositories

## Production Deployment

### Docker Build

```bash
./gradlew build
docker build -f src/main/docker/Dockerfile.jvm -t oportunidade-backend:latest .
```

### Docker Run

```bash
docker run -d \
  -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://db:5432/oportunidade \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=secret \
  oportunidade-backend:latest
```

### Environment Best Practices

- Use secrets management for credentials
- Enable SSL/TLS for database connections
- Configure proper logging levels
- Set up monitoring and alerting
- Regular database backups

## Support

For issues or questions:
- Create an issue on GitHub
- Contact: development team
- Documentation: See `docs/` folder

## License

[Your License Here]

## Contributors

- Development Team
- AppyPay Integration Team
