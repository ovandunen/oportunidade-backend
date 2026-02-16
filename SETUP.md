# Oportunidade Backend - Setup Guide

## Quick Start

This guide will help you set up the Oportunidade backend for local development.

**Prerequisites:**
- Java 17 or higher
- PostgreSQL 12 or higher
- Maven 3.8+ (or use included `./mvnw`)
- Git
- OpenSSL (for JWT key generation)

---

## 1. Clone Repository

```bash
git clone <repository-url>
cd oportunidade/recruiting
```

---

## 2. Database Setup

### Create PostgreSQL Database

```sql
-- Connect to PostgreSQL
psql -U postgres

-- Create database
CREATE DATABASE odoo_payments;

-- Create user (optional, for security)
CREATE USER oportunidade_app WITH PASSWORD 'secure_password';
GRANT ALL PRIVILEGES ON DATABASE odoo_payments TO oportunidade_app;

-- Exit
\q
```

### Verify Connection

```bash
psql -U postgres -d odoo_payments -c "SELECT version();"
```

---

## 3. Generate JWT Keys

JWT keys are used to sign document access tokens. **NEVER commit private keys to git.**

```bash
# Generate RSA key pair
./scripts/generate-jwt-keys.sh
```

This creates:
- `src/main/resources/privateKey.pem` (git-ignored, KEEP SECRET)
- `src/main/resources/publicKey.pem` (public key)

**⚠️ IMPORTANT**: Back up `privateKey.pem` securely. If lost, all existing tokens become invalid.

---

## 4. Configure Environment

### Copy Environment Template

```bash
cp .env.example .env
```

### Edit `.env` with Your Values

```bash
# Required - Database
DB_URL=jdbc:postgresql://localhost:5432/odoo_payments
DB_USERNAME=postgres
DB_PASSWORD=postgres

# Required - Odoo SaaS
ODOO_URL=https://your-instance.odoo.com
ODOO_DATABASE=your-database-name
ODOO_USERNAME=api-user
ODOO_PASSWORD=secure-odoo-password

# Required - JWT
JWT_PRIVATE_KEY_FILE=src/main/resources/privateKey.pem
JWT_ISSUER=recruiting-agency-backend

# Optional - Slack Alerts (leave empty to disable)
SLACK_WEBHOOK_URL=
SLACK_CHANNEL=#payment-alerts
```

### For Development (Mock Odoo)

If you don't have an Odoo instance, use these development settings:

```bash
ODOO_URL=http://localhost:8069
ODOO_DATABASE=odoo_dev
ODOO_USERNAME=admin
ODOO_PASSWORD=admin
```

---

## 5. Run Database Migrations

Flyway migrations run automatically on startup, but you can also run them manually:

```bash
./mvnw flyway:migrate
```

**Expected Output:**
```
[INFO] Successfully applied 2 migration(s)
[INFO]   - V1: create_webhook_tables
[INFO]   - V2: add_payment_document_mapping
```

### Verify Tables Created

```sql
psql -d odoo_payments -c "\dt"
```

**Expected Tables:**
- `orders`
- `order_candidates`
- `order_odoo_documents`
- `employer_references`
- `payment_transactions`
- `webhook_events`
- `flyway_schema_history`

---

## 6. Insert Test Data (Optional)

For local development, insert test employer references:

```sql
psql -d odoo_payments <<EOF
INSERT INTO employer_references (reference_code, employer_id, employer_email, company_name, created_at, is_active)
VALUES 
    ('TEST-REF-001', 'EMP-001', 'employer1@test.com', 'Test Company A', NOW(), true),
    ('TEST-REF-002', 'EMP-002', 'employer2@test.com', 'Test Company B', NOW(), true),
    ('TEST-REF-003', 'EMP-003', 'employer3@test.com', 'Test Company C', NOW(), true)
ON CONFLICT (reference_code) DO NOTHING;
EOF
```

---

## 7. Build Project

### Clean Build

```bash
./mvnw clean package
```

### Skip Tests (faster)

```bash
./mvnw clean package -DskipTests
```

**Expected Output:**
```
[INFO] BUILD SUCCESS
[INFO] Total time: 45.123 s
```

---

## 8. Run Application

### Development Mode (with live reload)

```bash
./mvnw quarkus:dev
```

**Features in Dev Mode:**
- Live reload on code changes
- Dev UI at http://localhost:8080/q/dev/
- Swagger UI at http://localhost:8080/q/swagger-ui/
- Health checks at http://localhost:8080/q/health

### Production Mode

```bash
java -jar target/quarkus-app/quarkus-run.jar
```

---

## 9. Verify Setup

### Check Application Health

```bash
curl http://localhost:8080/q/health
```

**Expected Response:**
```json
{
  "status": "UP",
  "checks": [...]
}
```

### Check Swagger UI

Open browser: http://localhost:8080/q/swagger-ui/

You should see the API documentation.

### Test Odoo Connection (if configured)

```bash
curl -X POST "${ODOO_URL}/web/session/authenticate" \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "params": {
      "db": "'"${ODOO_DATABASE}"'",
      "login": "'"${ODOO_USERNAME}"'",
      "password": "'"${ODOO_PASSWORD}"'"
    }
  }'
```

**Expected**: JSON response with `session_id`

---

## 10. Run Tests

### All Tests

```bash
./mvnw test
```

### Specific Test Class

```bash
./mvnw test -Dtest=DocumentTokenServiceTest
```

### Integration Tests Only

```bash
./mvnw test -Dtest=*IT
```

---

## Troubleshooting

### Issue: Database Connection Failed

**Error**: `Unable to connect to database`

**Solution**:
1. Verify PostgreSQL is running: `systemctl status postgresql`
2. Check credentials in `.env`
3. Test connection: `psql -U postgres -d odoo_payments`

---

### Issue: JWT Key Not Found

**Error**: `privateKey.pem not found`

**Solution**:
```bash
./scripts/generate-jwt-keys.sh
```

Verify file exists:
```bash
ls -la src/main/resources/*.pem
```

---

### Issue: Flyway Migration Failed

**Error**: `Migration checksum mismatch`

**Solution** (⚠️ DEVELOPMENT ONLY):
```bash
# Drop and recreate database
psql -U postgres -c "DROP DATABASE IF EXISTS odoo_payments;"
psql -U postgres -c "CREATE DATABASE odoo_payments;"

# Rerun migrations
./mvnw flyway:migrate
```

---

### Issue: Port 8080 Already in Use

**Error**: `Port 8080 is already in use`

**Solution**:
```bash
# Change port in .env
BASE_URL=http://localhost:8081

# Or kill process using port
lsof -ti:8080 | xargs kill -9
```

---

## Development Workflow

### 1. Start Development Server

```bash
./mvnw quarkus:dev
```

### 2. Make Code Changes

Quarkus will automatically reload changes.

### 3. Test Changes

```bash
curl -X POST http://localhost:8080/webhooks/appypay \
  -H "Content-Type: application/json" \
  -d '{
    "id": "test-123",
    "status": "Success",
    "reference": {"referenceNumber": "TEST-REF-001"}
  }'
```

### 4. Check Logs

Logs appear in terminal running `quarkus:dev`

### 5. Query Database

```bash
psql -d odoo_payments -c "SELECT * FROM orders ORDER BY created_date DESC LIMIT 5;"
```

---

## Project Structure

```
recruiting/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── ao/co/oportunidade/       # Main application code
│   │   │   └── solutions/envision/       # Shared libraries
│   │   └── resources/
│   │       ├── application.yml           # Configuration
│   │       ├── db/migration/             # Flyway migrations
│   │       ├── privateKey.pem           # JWT private key (git-ignored)
│   │       └── publicKey.pem            # JWT public key
│   └── test/
│       └── java/                         # Test code
├── scripts/
│   └── generate-jwt-keys.sh             # Key generation script
├── .env                                  # Environment config (git-ignored)
├── .env.example                          # Environment template
├── .gitignore                            # Git ignore rules
└── pom.xml                               # Maven dependencies
```

---

## Next Steps

After setup is complete:

1. **Review Analysis Documents**:
   - `EXECUTIVE_SUMMARY.md` - Business context
   - `ACTIONABLE_TASKS.md` - Implementation tasks
   - `CODE_ANALYSIS_AND_TEST_STRATEGY.md` - Test strategy

2. **Configure Production Environment**:
   - Set up real Odoo instance
   - Configure SMTP for emails
   - Set up Slack webhooks
   - Use proper secrets management (Vault, AWS Secrets Manager)

3. **Deploy to Staging**:
   - Build Docker image
   - Deploy to staging environment
   - Run integration tests
   - Validate with real Odoo data

---

## Support

- **Documentation**: See `/docs` folder
- **Issues**: [GitHub Issues]
- **Slack**: #oportunidade-dev

---

**Setup Complete!** 🎉

Your development environment is ready. Start the server with `./mvnw quarkus:dev` and visit http://localhost:8080/q/dev/.
