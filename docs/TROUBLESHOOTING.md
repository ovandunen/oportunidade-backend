# Troubleshooting Guide - AppyPay Webhook Integration

## Common Issues and Solutions

### 1. Webhooks Not Being Received

**Symptoms:**
- No entries in `webhook_events` table
- AppyPay shows webhook delivery failures
- No logs showing webhook receipt

**Possible Causes:**
- Application not running
- Firewall blocking requests
- Incorrect webhook URL configured in AppyPay

**Solutions:**

1. Check if application is running:
```bash
curl http://localhost:8080/webhooks/appypay/health
```

2. Check application logs:
```bash
tail -f logs/application.log | grep webhook
```

3. Verify webhook URL in AppyPay dashboard matches your endpoint

4. Test webhook locally:
```bash
curl -X POST http://localhost:8080/webhooks/appypay \
  -H "Content-Type: application/json" \
  -d '{"id": "test", "merchantTransactionId": "TEST", ...}'
```

---

### 2. Webhooks Received But Not Processing

**Symptoms:**
- Entries in `webhook_events` table with status `RECEIVED`
- No corresponding orders or transactions created
- Processing never completes

**Possible Causes:**
- Async processing not working
- Database connection issues
- Errors in payment service logic

**Solutions:**

1. Check webhook event status:
```sql
SELECT id, appypayTransactionId, processingStatus, error_message, retry_count
FROM webhook_events
WHERE processingStatus != 'PROCESSED'
ORDER BY created_date DESC;
```

2. Check application logs for errors:
```bash
grep -i "error\|exception" logs/application.log | tail -20
```

3. Verify reactive messaging configuration:
```properties
# In application.properties
mp.messaging.outgoing.webhook-events.connector=smallrye-in-memory
mp.messaging.incoming.webhook-events.connector=smallrye-in-memory
```

4. Restart the application:
```bash
./gradlew quarkusDev
```

---

### 3. Duplicate Order Creation

**Symptoms:**
- Multiple orders with same `merchantTransactionId`
- Duplicate payment transactions

**Possible Causes:**
- Idempotency check not working
- Database unique constraint missing
- Concurrent webhook processing

**Solutions:**

1. Verify unique constraint exists:
```sql
SELECT constraint_name, constraint_type
FROM information_schema.table_constraints
WHERE table_name = 'orders' AND constraint_type = 'UNIQUE';
```

2. Check idempotency logic in logs:
```bash
grep "already processed" logs/application.log
```

3. Verify webhook events for duplicates:
```sql
SELECT appypayTransactionId, COUNT(*)
FROM webhook_events
GROUP BY appypayTransactionId
HAVING COUNT(*) > 1;
```

---

### 4. Database Connection Failures

**Symptoms:**
- Application fails to start
- Errors mentioning database connection
- Health check shows database DOWN

**Possible Causes:**
- PostgreSQL not running
- Incorrect database credentials
- Database doesn't exist

**Solutions:**

1. Check PostgreSQL status:
```bash
systemctl status postgresql
# or
pg_isready
```

2. Verify database exists:
```bash
psql -U postgres -c "\l" | grep oportunidade
```

3. Test connection:
```bash
psql -U oportunidade_user -d oportunidade -c "SELECT 1;"
```

4. Check environment variables:
```bash
echo $DB_URL
echo $DB_USERNAME
echo $DB_PASSWORD
```

5. Update configuration:
```bash
export DB_URL=jdbc:postgresql://localhost:5432/oportunidade
export DB_USERNAME=postgres
export DB_PASSWORD=your_password
```

---

### 5. Flyway Migration Failures

**Symptoms:**
- Application won't start
- Errors mentioning Flyway or migrations
- Missing database tables

**Possible Causes:**
- Migration script errors
- Database schema mismatch
- Baseline not set

**Solutions:**

1. Check Flyway status:
```sql
SELECT * FROM flyway_schema_history;
```

2. Repair Flyway:
```bash
./gradlew quarkusFlywayRepair
```

3. Baseline database (for existing databases):
```bash
./gradlew quarkusFlywayBaseline
```

4. Clean and migrate (⚠️ DELETES ALL DATA):
```bash
./gradlew quarkusFlywayClean
./gradlew quarkusFlywayMigrate
```

---

### 6. Order Status Not Updating

**Symptoms:**
- Orders stuck in PENDING status
- Success webhooks not updating order to PAID

**Possible Causes:**
- Order not found by merchant transaction ID
- Transaction boundary issues
- Logic error in payment service

**Solutions:**

1. Check if order exists:
```sql
SELECT id, merchantTransactionId, status, updated_date
FROM orders
WHERE merchantTransactionId = 'YOUR-ORDER-ID';
```

2. Check payment transactions:
```sql
SELECT id, order_id, appypayTransactionId, status, created_date
FROM payment_transactions
WHERE order_id = 'ORDER-UUID';
```

3. Review payment service logic:
- Verify `findOrCreateOrder()` logic
- Check status mapping in `handleSuccessfulPayment()`

4. Check logs for specific order:
```bash
grep "ORDER-YOUR-ID" logs/application.log
```

---

### 7. Reference Linking Not Working

**Symptoms:**
- Orders created but `reference_id` is NULL
- Payment references not linking to orders

**Possible Causes:**
- Reference doesn't exist in database
- Reference number mismatch
- Query error in repository

**Solutions:**

1. Check if reference exists:
```sql
SELECT * FROM reference_entity
WHERE reference_number = 'YOUR-REF-NUMBER';
```

2. Verify linking logic:
```java
// In PaymentService
referenceRepository.findByReferenceNumber(refInfo.getReferenceNumber())
    .ifPresent(ref -> order.setReferenceId(ref.getId()));
```

3. Check logs:
```bash
grep "reference" logs/application.log | grep -i "not found\|error"
```

---

### 8. High Memory Usage / Performance Issues

**Symptoms:**
- Application slow to respond
- High memory consumption
- Webhook processing delays

**Possible Causes:**
- Too many async tasks
- Database connection pool exhausted
- Insufficient resources

**Solutions:**

1. Check memory usage:
```bash
jps -l
jstat -gc <PID> 1000
```

2. Monitor metrics:
```bash
curl http://localhost:8080/q/metrics | grep jvm
```

3. Adjust connection pool:
```properties
# In application.properties
quarkus.datasource.jdbc.max-size=20
quarkus.datasource.jdbc.min-size=5
```

4. Increase heap size:
```bash
java -Xmx1g -Xms512m -jar build/quarkus-app/quarkus-run.jar
```

---

### 9. Test Failures

**Symptoms:**
- Tests fail with database errors
- Integration tests timeout
- Testcontainers issues

**Possible Causes:**
- Docker not running
- Port conflicts
- Test database configuration

**Solutions:**

1. Check Docker status:
```bash
docker ps
docker info
```

2. Clean test environment:
```bash
./gradlew clean test
```

3. Run specific test:
```bash
./gradlew test --tests WebhookEventServiceTest
```

4. Check test logs:
```bash
cat build/reports/tests/test/index.html
```

---

### 10. JSON Parsing Errors

**Symptoms:**
- 400 Bad Request responses
- Jackson deserialization errors
- Null pointer exceptions

**Possible Causes:**
- Malformed JSON payload
- Missing required fields
- Date format mismatch

**Solutions:**

1. Validate JSON payload:
```bash
echo '{"id": "test", ...}' | jq .
```

2. Check DTO annotations:
```java
@JsonProperty("fieldName")
@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
```

3. Test deserialization:
```java
ObjectMapper mapper = new ObjectMapper();
AppyPayWebhookPayload payload = mapper.readValue(json, AppyPayWebhookPayload.class);
```

4. Enable Jackson debug logging:
```properties
quarkus.log.category."com.fasterxml.jackson".level=DEBUG
```

---

## Debugging Workflow

### Step 1: Identify the Problem

1. Check application health:
```bash
curl http://localhost:8080/q/health
```

2. Review recent logs:
```bash
tail -100 logs/application.log
```

3. Query database for webhook status:
```sql
SELECT * FROM webhook_events ORDER BY created_date DESC LIMIT 10;
```

### Step 2: Isolate the Issue

1. Test webhook endpoint directly:
```bash
curl -X POST http://localhost:8080/webhooks/appypay \
  -H "Content-Type: application/json" \
  -d @test-payload.json
```

2. Check specific component logs:
```bash
grep "PaymentService\|WebhookProcessor" logs/application.log
```

3. Query related database tables:
```sql
-- Get webhook event
SELECT * FROM webhook_events WHERE id = 'event-id';

-- Get associated order
SELECT * FROM orders WHERE merchantTransactionId = 'ORDER-ID';

-- Get transactions
SELECT * FROM payment_transactions WHERE order_id = 'order-uuid';
```

### Step 3: Fix and Verify

1. Apply fix
2. Restart application
3. Test with sample payload
4. Verify in database
5. Check logs for success

---

## Useful Queries

### Find Failed Webhooks
```sql
SELECT id, appypayTransactionId, error_message, retry_count, created_date
FROM webhook_events
WHERE processingStatus = 'FAILED'
ORDER BY created_date DESC;
```

### Find Pending Orders
```sql
SELECT id, merchantTransactionId, amount, currency, created_date
FROM orders
WHERE status = 'PENDING'
  AND created_date < NOW() - INTERVAL '1 hour'
ORDER BY created_date;
```

### Webhook Processing Stats
```sql
SELECT 
    processingStatus,
    COUNT(*) as count,
    AVG(EXTRACT(EPOCH FROM (processed_at - received_at))) as avg_processing_time_seconds
FROM webhook_events
WHERE received_at > NOW() - INTERVAL '24 hours'
GROUP BY processingStatus;
```

### Dead Letter Queue
```sql
SELECT * FROM webhook_events
WHERE processingStatus = 'DEAD_LETTER'
ORDER BY updated_date DESC;
```

---

## Getting Help

If you're still experiencing issues:

1. **Enable DEBUG logging:**
```properties
quarkus.log.category."ao.co.oportunidade.webhook".level=DEBUG
```

2. **Collect diagnostic information:**
```bash
# Application logs
tail -500 logs/application.log > diagnostic.log

# Database state
psql -U postgres -d oportunidade -c "
SELECT * FROM webhook_events WHERE id = 'problem-event-id';
" >> diagnostic.log

# Health check
curl http://localhost:8080/q/health >> diagnostic.log
```

3. **Create GitHub issue** with:
   - Error message
   - Stack trace
   - Diagnostic logs
   - Steps to reproduce
   - Expected vs actual behavior

4. **Contact support** with issue number and diagnostic information
