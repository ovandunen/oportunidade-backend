# Phase 1 Code Review - Tasks 0-3

**Review Date**: 2026-02-16  
**Implementation**: Tasks 0-3 (Environment Setup through Token Generation)  
**Status**: Ready for Review  
**Estimated Review Time**: 30-45 minutes

---

## 📖 Review Overview

This document guides you through reviewing the Phase 1 implementation (Tasks 0-3). Each section includes:
- **What Changed**: Summary of modifications
- **Why It Changed**: Business justification
- **Files to Review**: Specific files and line numbers
- **Verification Steps**: How to test the changes
- **Potential Issues**: What to watch for

---

## 🎯 Review Objectives

By the end of this review, you should be confident that:

1. ✅ Security is properly configured (no secrets in git)
2. ✅ Odoo authentication will work with correct credentials
3. ✅ Database schema supports payment-to-document mapping
4. ✅ Token generation integrates with payment processing
5. ✅ Code quality meets project standards

---

## 📂 Files Changed Summary

| File | Status | Lines | Priority | Description |
|------|--------|-------|----------|-------------|
| `.gitignore` | Modified | +15 | 🔴 Critical | Excludes secrets |
| `.env.example` | Created | 60 | 🔴 Critical | Config template |
| `.env` | Created | 60 | 🔴 Critical | Dev config (git-ignored) |
| `scripts/generate-jwt-keys.sh` | Created | 40 | 🔴 Critical | Key generation |
| `application.yml` | Modified | +50 | 🔴 Critical | Odoo/JWT config |
| `OdooDocumentClient.java` | Modified | ~10 | 🔴 Critical | Fix auth config |
| `EmployerReference.java` | Created | 95 | 🔴 Critical | Employer lookup |
| `PackageType.java` | Created | 60 | 🔴 Critical | Token expiration |
| `Order.java` | Modified | +30 | 🔴 Critical | Add mapping fields |
| `OrderEntity.java` | Modified | +50 | 🔴 Critical | JPA mappings |
| `V2__add_payment_document_mapping.sql` | Created | 120 | 🔴 Critical | DB migration |
| `DocumentTokenService.java` | Created | 150 | 🔴 Critical | JWT generation |
| `NotificationService.java` | Created | 80 | 🟡 High | Email stub |
| `PaymentProcessService.java` | Modified | +120 | 🔴 Critical | Token integration |
| `SETUP.md` | Created | 400 | 🟢 Medium | Setup guide |
| `PHASE_1_STATUS.md` | Created | 500 | 🟢 Medium | Status report |

**Total**: 16 files, ~1,280 lines of code

---

## 🔐 TASK 0 REVIEW: Environment Setup & Security

### What Changed

1. **JWT Key Generation**
   - Created RSA key pair for signing tokens
   - Private key is git-ignored
   - Public key included in repo

2. **Environment Configuration**
   - `.env.example` template with 25 variables
   - `.env` for local development (git-ignored)
   - No hardcoded secrets in code

3. **Git Security**
   - Updated `.gitignore` to exclude all secrets
   - Added patterns for `*.pem`, `.env`, local configs

### Files to Review

#### 1. `.gitignore` (Lines 1-25)
```bash
# Location: /recruiting/.gitignore
```

**Check**:
- [ ] Includes `*.pem` pattern
- [ ] Includes `.env` and `.env.*` patterns
- [ ] Includes `application-local.*` patterns
- [ ] No overly broad patterns (like `*`)

**Test**:
```bash
# Verify git ignores secrets
git status
# Should NOT show: privateKey.pem, .env, application-local.yml
```

---

#### 2. `scripts/generate-jwt-keys.sh` (Full file)
```bash
# Location: /recruiting/scripts/generate-jwt-keys.sh
```

**Check**:
- [ ] Creates 2048-bit RSA key
- [ ] Sets restrictive permissions (600 for private key)
- [ ] Clear security warnings in output
- [ ] Handles errors gracefully

**Test**:
```bash
# Run script
./scripts/generate-jwt-keys.sh

# Verify keys created
ls -la src/main/resources/*.pem
# Should show: privateKey.pem (600), publicKey.pem (644)

# Verify key format
openssl rsa -in src/main/resources/privateKey.pem -check
# Should output: "RSA key ok"
```

---

#### 3. `.env.example` (Full file)
```bash
# Location: /recruiting/.env.example
```

**Check**:
- [ ] All variables have placeholder values (no real secrets)
- [ ] Comments explain purpose of each section
- [ ] Includes all required variables:
  - JWT configuration (3 variables)
  - Odoo configuration (6 variables)
  - Database configuration (3 variables)
  - Retry configuration (3 variables)
  - Slack configuration (3 variables)
  - Application configuration (2 variables)

**Test**:
```bash
# Verify no real secrets
grep -i "password.*=" .env.example
# All values should be placeholders like "your-password-here"
```

---

#### 4. `.env` (Full file) - **DO NOT COMMIT**
```bash
# Location: /recruiting/.env (git-ignored)
```

**Check**:
- [ ] File exists but is git-ignored
- [ ] Contains development values
- [ ] Database points to local PostgreSQL
- [ ] Odoo points to local instance or staging

**Test**:
```bash
# Verify git ignores it
git status
# Should NOT show .env

# Verify it's not in git
git ls-files | grep "^\.env$"
# Should return nothing
```

---

### Security Review Checklist

- [ ] No secrets in `application.yml`
- [ ] No secrets in Java source files
- [ ] `privateKey.pem` is git-ignored
- [ ] `.env` is git-ignored
- [ ] `.env.example` has no real credentials
- [ ] Database passwords not hardcoded
- [ ] JWT secret not hardcoded
- [ ] Slack webhook URL not hardcoded

**If any checklist item fails, STOP and fix before continuing.**

---

## 🔧 TASK 1 REVIEW: Odoo Authentication Fix

### What Changed

**Problem**: `OdooDocumentClient` was reading database credentials instead of Odoo credentials, causing 401 authentication failures.

**Solution**: Updated configuration properties to use dedicated Odoo credentials.

### Files to Review

#### 1. `application.yml` (Lines 84-93)
```yaml
# Location: /recruiting/src/main/resources/application.yml
# Lines: 84-93
```

**Review**:
```yaml
odoo:
  url: ${ODOO_URL:http://localhost:8069}
  database: ${ODOO_DATABASE:odoo_dev}
  username: ${ODOO_USERNAME:admin}
  password: ${ODOO_PASSWORD:admin}
  timeout: ${ODOO_TIMEOUT:30000}
  max-retries: ${ODOO_MAX_RETRIES:3}
```

**Check**:
- [ ] Uses environment variables (not hardcoded)
- [ ] Has sensible defaults for development
- [ ] Timeout is 30 seconds (reasonable)
- [ ] Max retries is 3 (appropriate)

**Potential Issues**:
- ⚠️ Default credentials (`admin/admin`) are insecure
- ⚠️ Production must override these values
- ⚠️ No TLS/SSL configuration yet

---

#### 2. `OdooDocumentClient.java` (Lines 17-27)
```java
// Location: /recruiting/src/main/java/solutions/envision/odoo/document/OdooDocumentClient.java
// Lines: 17-27
```

**Review**:
```java
@ConfigProperty(name = "odoo.url")
String odooUrl;

@ConfigProperty(name = "odoo.database")
String database;

@ConfigProperty(name = "odoo.username")
String username;

@ConfigProperty(name = "odoo.password")
String password;
```

**Check**:
- [ ] Property names match `application.yml`
- [ ] No default values (relies on config)
- [ ] Field names are clear and descriptive

**BEFORE (Incorrect)**:
```java
@ConfigProperty(name = "quarkus.datasource.db-kind")  // WRONG!
String database;  // Would be "postgresql", not Odoo database name
```

**AFTER (Correct)**:
```java
@ConfigProperty(name = "odoo.database")  // CORRECT
String database;  // Now gets actual Odoo database name
```

---

### Verification Steps

#### Manual Test: Odoo Authentication

```bash
# 1. Set Odoo credentials in .env
ODOO_URL=https://your-instance.odoo.com
ODOO_DATABASE=your-database
ODOO_USERNAME=your-username
ODOO_PASSWORD=your-password

# 2. Test authentication with curl
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

# Expected success response:
# {"jsonrpc":"2.0","id":null,"result":{"session_id":"...", "uid": 2, ...}}

# Expected failure (wrong credentials):
# {"jsonrpc":"2.0","id":null,"error":{"code":100,"message":"Access Denied"}}
```

#### Code Review Questions

1. **Q**: What happens if `ODOO_URL` is not set?
   **A**: Falls back to default `http://localhost:8069`

2. **Q**: Can the same credentials be used for database and Odoo?
   **A**: No, they're now separate. Database uses `DB_*`, Odoo uses `ODOO_*`

3. **Q**: Where is the Odoo database name used?
   **A**: In XML-RPC authentication call (line 41 of `OdooDocumentClient.java`)

---

## 🗄️ TASK 2 REVIEW: Payment-Document Mapping

### What Changed

**Problem**: No way to map payments to specific employers and their candidate documents.

**Solution**: 
1. Created `EmployerReference` entity for reference code lookup
2. Added fields to `Order` entity for employer and document mapping
3. Created database schema with junction tables
4. Defined `PackageType` enum for token expiration

### Files to Review

#### 1. `EmployerReference.java` (Full file - 95 lines)
```java
// Location: /recruiting/src/main/java/ao/co/oportunidade/employer/model/EmployerReference.java
```

**Key Points**:
- Panache entity (simplified JPA)
- Maps payment `referenceCode` to internal `employerId`
- Includes `isActive` flag for deactivation without deletion
- Has finder methods: `findByReferenceCode()`, `findByEmployerId()`

**Check**:
- [ ] `referenceCode` has unique constraint
- [ ] `isActive` defaults to `true`
- [ ] Finder methods filter by `isActive = true`
- [ ] Timestamps are properly managed

**Sample Data**:
```sql
-- This is how employer references are created
INSERT INTO employer_references (reference_code, employer_id, employer_email, company_name, created_at, is_active)
VALUES ('REF-001', 'EMP-001', 'hr@company.com', 'ACME Corp', NOW(), true);
```

---

#### 2. `PackageType.java` (Full file - 60 lines)
```java
// Location: /recruiting/src/main/java/ao/co/oportunidade/order/model/PackageType.java
```

**Review Enum Values**:
```java
BASIC(24),       // 24 hours
STANDARD(48),    // 48 hours  
PREMIUM(72),     // 72 hours
ENTERPRISE(168); // 7 days
```

**Check**:
- [ ] Expiration values match business requirements
- [ ] `fromString()` method handles case-insensitivity
- [ ] Clear validation error messages
- [ ] Javadoc explains each package type

**Business Rule Validation**:
- Is 24 hours enough for BASIC? ✓ Quick reviews
- Is 7 days appropriate for ENTERPRISE? ✓ Team evaluations
- Should we add more package types? Consider for Phase 2

---

#### 3. `Order.java` (Lines 31-66 - New fields)
```java
// Location: /recruiting/src/main/java/ao/co/oportunidade/order/model/Order.java
// Lines: 31-66
```

**Review New Fields**:
```java
private String employerId;              // Looked up from EmployerReference
private String employerEmail;           // For notifications
private PackageType packageType;        // Determines token expiration
private List<String> candidateIds;      // Specific candidates to unlock
private List<String> odooDocumentIds;   // Odoo ir.attachment IDs
private String referenceCode;           // Payment reference from webhook
```

**Check**:
- [ ] All new fields have Javadoc comments
- [ ] Field names are clear and consistent
- [ ] Lists use generic types (`List<String>`)
- [ ] No primitive types that could cause NPE

**Validation Rules** (not yet enforced):
- `employerId` - Required for token generation
- `employerEmail` - Required for notifications
- `packageType` - Required, no null
- `candidateIds` - Must have at least 1 element
- `odooDocumentIds` - Can be empty (populated later)

---

#### 4. `OrderEntity.java` (Lines 80-113 - JPA mappings)
```java
// Location: /recruiting/src/main/java/ao/co/oportunidade/order/entity/OrderEntity.java
// Lines: 80-113
```

**Review JPA Annotations**:

```java
@Column(name = "employer_id", length = 255)
private String employerId;

@Column(name = "package_type", length = 50)
@Enumerated(EnumType.STRING)  // Stores "BASIC", not 0,1,2,3
private String packageType;

@ElementCollection
@CollectionTable(name = "order_candidates", joinColumns = @JoinColumn(name = "order_id"))
@Column(name = "candidate_id", length = 255)
private List<String> candidateIds;
```

**Check**:
- [ ] Column lengths are appropriate (255 for IDs, 50 for enum)
- [ ] `@ElementCollection` for lists (creates junction tables)
- [ ] `@Enumerated(EnumType.STRING)` not ORDINAL (safer for refactoring)
- [ ] Junction tables have proper foreign keys

**Potential Issues**:
- ⚠️ `candidateIds` and `odooDocumentIds` use `@ElementCollection` which can have performance issues with large lists
- ✅ For expected use case (5-20 candidates per order), this is fine
- 📝 If orders have 100+ candidates, consider separate entity

---

#### 5. `V2__add_payment_document_mapping.sql` (Full file - 120 lines)
```sql
# Location: /recruiting/src/main/resources/db/migration/V2__add_payment_document_mapping.sql
```

**Review Migration Structure**:

**Section 1: employer_references table**
```sql
CREATE TABLE employer_references (
    id BIGSERIAL PRIMARY KEY,
    reference_code VARCHAR(255) UNIQUE NOT NULL,
    employer_id VARCHAR(255) NOT NULL,
    employer_email VARCHAR(255),
    company_name VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);
```

**Check**:
- [ ] `BIGSERIAL` for auto-incrementing ID
- [ ] `UNIQUE` constraint on `reference_code`
- [ ] `NOT NULL` on required fields
- [ ] Timestamp default to `NOW()`
- [ ] Comments added via `COMMENT ON`

**Section 2: orders table columns**
```sql
ALTER TABLE orders ADD COLUMN employer_id VARCHAR(255);
ALTER TABLE orders ADD COLUMN employer_email VARCHAR(255);
ALTER TABLE orders ADD COLUMN package_type VARCHAR(50);
ALTER TABLE orders ADD COLUMN reference_code VARCHAR(255);
```

**Check**:
- [ ] Uses `IF NOT EXISTS` pattern (idempotent)
- [ ] Column types match entity fields
- [ ] No `NOT NULL` constraints (allows gradual migration)

**Section 3: Junction tables**
```sql
CREATE TABLE order_candidates (
    order_id UUID NOT NULL,
    candidate_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (order_id, candidate_id),
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);
```

**Check**:
- [ ] Composite primary key prevents duplicates
- [ ] Foreign key with `ON DELETE CASCADE` (cleans up orphans)
- [ ] Indexes created for both directions of lookup

**Section 4: Indexes**
```sql
CREATE INDEX idx_employer_ref_code ON employer_references(reference_code);
CREATE INDEX idx_orders_employer ON orders(employer_id);
CREATE INDEX idx_order_candidates_order ON order_candidates(order_id);
```

**Check**:
- [ ] Index naming follows pattern: `idx_{table}_{column}`
- [ ] Indexes on foreign keys (performance)
- [ ] Indexes on frequently queried columns

---

### Database Review Checklist

**Before Running Migration**:
- [ ] PostgreSQL database exists
- [ ] User has CREATE TABLE privileges
- [ ] V1 migration already applied
- [ ] Backup database (safety)

**Run Migration**:
```bash
# Check current status
./mvnw flyway:info

# Run V2 migration
./mvnw flyway:migrate

# Verify success
psql -d odoo_payments -c "\dt"
# Should show: employer_references, order_candidates, order_odoo_documents
```

**After Migration**:
- [ ] All 3 new tables exist
- [ ] orders table has 4 new columns
- [ ] All 9 indexes created
- [ ] Comments added to columns
- [ ] No migration errors

**Rollback Plan** (if needed):
```sql
-- Drop new tables (CASCADE removes foreign keys)
DROP TABLE IF EXISTS order_odoo_documents CASCADE;
DROP TABLE IF EXISTS order_candidates CASCADE;
DROP TABLE IF EXISTS employer_references CASCADE;

-- Remove columns from orders (careful!)
ALTER TABLE orders DROP COLUMN IF EXISTS employer_id;
ALTER TABLE orders DROP COLUMN IF EXISTS employer_email;
ALTER TABLE orders DROP COLUMN IF EXISTS package_type;
ALTER TABLE orders DROP COLUMN IF EXISTS reference_code;

-- Delete migration record
DELETE FROM flyway_schema_history WHERE version = '2';
```

---

## 🎫 TASK 3 REVIEW: Payment → Token Generation

### What Changed

**Problem**: Successful payments did not generate document access tokens.

**Solution**: 
1. Created `DocumentTokenService` to generate JWT tokens
2. Created `NotificationService` stub for emails (Phase 1: logging)
3. Enhanced `PaymentProcessService` to integrate token generation

### Files to Review

#### 1. `DocumentTokenService.java` (Full file - 150 lines)
```java
// Location: /recruiting/src/main/java/solutions/envision/odoo/document/service/DocumentTokenService.java
```

**Key Methods**:

**`generateAccessToken(Order order)`** (Lines 45-75)
```java
String token = Jwt.issuer(jwtIssuer)
    .subject(order.getEmployerEmail())
    .claim("orderId", order.getId().toString())
    .claim("employerId", order.getEmployerId())
    .claim("packageType", order.getPackageType().name())
    .claim("candidateIds", order.getCandidateIds())
    .claim("odooDocumentIds", order.getOdooDocumentIds())
    .claim("tokenType", "document-access")
    .claim("multiUse", true)  // ✅ Multi-use token
    .expiresIn(Duration.ofHours(expirationHours))
    .issuedAt(Instant.now())
    .sign();
```

**Check JWT Claims**:
- [ ] `iss` (issuer) - Identifies our system
- [ ] `sub` (subject) - Employer email
- [ ] `orderId` - For audit trail
- [ ] `employerId` - Who purchased access
- [ ] `candidateIds` - Which candidates
- [ ] `odooDocumentIds` - Which documents
- [ ] `multiUse` - Allows multiple downloads
- [ ] `exp` (expiration) - Based on package type
- [ ] `iat` (issued at) - When token created

**Security Review**:
- ✅ Token is signed (prevents tampering)
- ✅ Has expiration (not valid forever)
- ✅ Includes employer ID (authorization check)
- ✅ Multi-use but audited (see Task 5)
- ⚠️ No revocation mechanism (Phase 2)
- ⚠️ No rate limiting on token usage (Phase 2)

**Validation Logic** (`validateOrder()` - Lines 105-136):
```java
if (order.getEmployerId() == null || order.getEmployerId().trim().isEmpty()) {
    throw new IllegalArgumentException("Employer ID is required");
}
```

**Check Validations**:
- [ ] Order cannot be null
- [ ] Order ID required
- [ ] Employer ID required (not blank)
- [ ] Employer email required (not blank)
- [ ] Package type required
- [ ] At least 1 candidate ID required

---

#### 2. `NotificationService.java` (Full file - 80 lines)
```java
// Location: /recruiting/src/main/java/ao/co/oportunidade/payment/service/NotificationService.java
```

**Phase 1 Implementation** (Lines 38-70):
```java
LOG.infof("===== DOCUMENT ACCESS EMAIL =====");
LOG.infof("TO: %s", employerEmail);
LOG.infof("SUBJECT: Your %s Package - %d Candidate Documents Ready", 
    packageType.name(), candidateCount);
LOG.infof("Download Link: %s", downloadUrl);
LOG.infof("Valid for %d hours", packageType.getExpirationHours());
// ... detailed email body logged ...
```

**Check**:
- [ ] Email content is logged (not sent)
- [ ] Includes all required information:
  - Employer email
  - Package type
  - Number of candidates
  - Download URL with token
  - Expiration notice
  - Security warnings
- [ ] Clear TODO comments for Phase 2 implementation

**Phase 2 TODO**:
```java
// TODO Phase 2: Implement actual email sending
// emailClient.send()
//     .to(employerEmail)
//     .subject(subject)
//     .body(htmlBody)
//     .send();
```

---

#### 3. `PaymentProcessService.java` (Lines 72-200+)
```java
// Location: /recruiting/src/main/java/ao/co/oportunidade/payment/service/PaymentProcessService.java
```

**Critical Review**: This file has the most significant changes.

**Enhanced `handleSuccessfulPayment()` Method** (Lines 72-120):

**Section A: Enrich Order** (Lines 74-76)
```java
// NEW: Phase 1 Enhancement
enrichOrderWithEmployerInfo(order, payload);
```

**Check**:
- [ ] Called before changing order status
- [ ] Populates employer ID, email, package type
- [ ] Looks up from EmployerReference
- [ ] Throws exception if reference not found

**Section B: Token Generation** (Lines 85-115)
```java
try {
    if (order.getEmployerId() != null && 
        order.getCandidateIds() != null && 
        !order.getCandidateIds().isEmpty()) {
        
        // Generate JWT token
        String accessToken = tokenService.generateAccessToken(order);
        
        // Generate download URL
        String downloadUrl = tokenService.generateDownloadUrl(accessToken);
        
        // Send email notification
        notificationService.sendDocumentAccessEmail(...);
        
        LOG.infof("Access notification sent to: %s", order.getEmployerEmail());
    }
} catch (Exception e) {
    LOG.errorf(e, "Failed to generate access token");
    // ✅ Don't fail the payment, just log error
}
```

**Check**:
- [ ] Wrapped in try-catch (token failure doesn't fail payment)
- [ ] Validates order has required fields
- [ ] Logs success/failure clearly
- [ ] Order status changed to `COMPLETED` (not just `PAID`)

---

**New Method: `enrichOrderWithEmployerInfo()`** (Lines 122-155)

```java
private void enrichOrderWithEmployerInfo(Order order, AppyPayWebhookPayload payload) {
    String referenceCode = extractReferenceCode(payload);
    
    EmployerReference employer = EmployerReference.findByReferenceCode(referenceCode);
    
    if (employer == null) {
        throw new IllegalStateException("Invalid employer reference: " + referenceCode);
    }
    
    order.setEmployerId(employer.getEmployerId());
    order.setEmployerEmail(employer.getEmployerEmail());
    order.setPackageType(determinePackageType(payload));
    order.setCandidateIds(extractCandidateIds(payload));
    order.setOdooDocumentIds(mapCandidatesToOdooDocuments(...));
}
```

**Check**:
- [ ] Looks up employer by reference code
- [ ] Throws exception if not found (fails payment)
- [ ] Sets all required order fields
- [ ] Logs enrichment details

---

**⚠️ PLACEHOLDER IMPLEMENTATIONS** - Review Carefully:

**1. `extractCandidateIds()` (Lines 170-180)**
```java
private List<String> extractCandidateIds(AppyPayWebhookPayload payload) {
    // TODO Phase 2: Extract from payload custom fields
    LOG.warnf("Using placeholder candidate IDs");
    return List.of("CAND-001", "CAND-002");  // ⚠️ PLACEHOLDER
}
```

**Status**: ⚠️ **Returns hardcoded test data**

**Phase 2 Implementation Options**:
1. Parse from `merchantTransactionId` pattern (e.g., "ORD-EMP001-CAND001-CAND002")
2. Extract from custom fields in AppyPay webhook payload
3. Pre-store during order creation (before payment)

**Action Required**:
- [ ] Decide on Phase 2 implementation approach
- [ ] Document expected payload format
- [ ] Update webhook examples with candidate IDs

---

**2. `mapCandidatesToOdooDocuments()` (Lines 182-192)**
```java
private List<String> mapCandidatesToOdooDocuments(List<String> candidateIds) {
    // TODO Phase 2: Query Odoo API
    return candidateIds.stream()
        .map(id -> "odoo_doc_" + id)  // ⚠️ PLACEHOLDER
        .toList();
}
```

**Status**: ⚠️ **Uses simple string concatenation**

**Phase 2 Implementation**:
```java
// Proper implementation:
return candidateIds.stream()
    .flatMap(candidateId -> {
        try {
            List<OdooDocument> docs = odooClient.getCandidateDocuments(
                Integer.parseInt(candidateId));
            return docs.stream().map(doc -> String.valueOf(doc.id()));
        } catch (Exception e) {
            LOG.errorf("Failed to fetch documents for candidate: %s", candidateId);
            return Stream.empty();
        }
    })
    .toList();
```

**Action Required**:
- [ ] Implement Odoo API call in Phase 2
- [ ] Handle API failures gracefully
- [ ] Cache document mappings (optional optimization)

---

**3. `determinePackageType()` (Lines 164-168)**
```java
private PackageType determinePackageType(AppyPayWebhookPayload payload) {
    // TODO: Extract from payload or amount-based logic
    return PackageType.STANDARD;  // ⚠️ ALWAYS RETURNS STANDARD
}
```

**Status**: ⚠️ **Always returns STANDARD (48-hour expiration)**

**Phase 2 Implementation Options**:
1. **Amount-based mapping**:
```java
BigDecimal amount = payload.getAmount();
if (amount.compareTo(new BigDecimal("1000")) < 0) return PackageType.BASIC;
if (amount.compareTo(new BigDecimal("5000")) < 0) return PackageType.STANDARD;
if (amount.compareTo(new BigDecimal("10000")) < 0) return PackageType.PREMIUM;
return PackageType.ENTERPRISE;
```

2. **Webhook custom field**:
```java
String packageTypeStr = payload.getCustomFields().get("package_type");
return PackageType.fromString(packageTypeStr);
```

3. **merchantTransactionId pattern**:
```java
// Parse "ORD-PREMIUM-123" → PackageType.PREMIUM
String[] parts = payload.getMerchantTransactionId().split("-");
return PackageType.fromString(parts[1]);
```

**Action Required**:
- [ ] Define business rule for package type determination
- [ ] Update AppyPay integration to include package type
- [ ] Document expected format

---

### Integration Flow Diagram

```
Payment Success Webhook
         ↓
handleSuccessfulPayment()
         ↓
extractReferenceCode(payload)  →  "TEST-REF-001"
         ↓
EmployerReference.findByReferenceCode()  →  Find "EMP-001"
         ↓
enrichOrderWithEmployerInfo()
    ├─ order.setEmployerId("EMP-001")
    ├─ order.setEmployerEmail("hr@company.com")
    ├─ order.setPackageType(STANDARD)  ⚠️ Placeholder
    ├─ order.setCandidateIds([...])    ⚠️ Placeholder
    └─ order.setOdooDocumentIds([...]) ⚠️ Placeholder
         ↓
order.setStatus(COMPLETED)
         ↓
DocumentTokenService.generateAccessToken(order)
         ↓
JWT Token Created:
{
  "iss": "recruiting-agency-backend",
  "sub": "hr@company.com",
  "orderId": "uuid...",
  "employerId": "EMP-001",
  "packageType": "STANDARD",
  "candidateIds": ["CAND-001", "CAND-002"],
  "exp": 1739490000,  // 48 hours from now
  "multiUse": true
}
         ↓
DocumentTokenService.generateDownloadUrl(token)
         ↓
URL: http://localhost:8080/api/documents/download?token=eyJhbG...
         ↓
NotificationService.sendDocumentAccessEmail()
         ↓
LOG: Email content (not actually sent in Phase 1)
         ↓
Payment Processing Complete ✅
```

---

## 🧪 Testing Instructions

### Prerequisites

1. **Database Setup**:
```bash
# Create database (if not exists)
psql -U postgres -c "CREATE DATABASE odoo_payments;"

# Run migrations
./mvnw flyway:migrate

# Insert test employer reference
psql -d odoo_payments <<EOF
INSERT INTO employer_references (reference_code, employer_id, employer_email, company_name, created_at, is_active)
VALUES ('TEST-REF-001', 'EMP-001', 'test@employer.com', 'Test Company', NOW(), true)
ON CONFLICT (reference_code) DO NOTHING;
EOF
```

2. **Environment Configuration**:
```bash
# Verify .env exists
cat .env | grep "ODOO_URL"

# Verify JWT keys exist
ls -la src/main/resources/*.pem
```

3. **Build Application**:
```bash
./mvnw clean compile
```

---

### Test 1: Verify Configuration Loads

**Purpose**: Ensure all configuration is properly read.

```bash
# Start application
./mvnw quarkus:dev

# Check logs for configuration loading
# Should see:
# ✅ "Flyway migration applied successfully"
# ✅ "Application started in Xs"
# ❌ No errors about missing configuration
```

**Expected Output**:
```
__  ____  __  _____   ___  __ ____  ______
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/
 -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \   
--\___\_\____/_/ |_/_/|_/_/|_|\____/___/   
INFO  [io.quarkus] Quarkus X.XX.X started in X.XXXs
INFO  [io.quarkus] Profile dev activated. Live Coding activated.
```

---

### Test 2: Database Schema Verification

**Purpose**: Confirm all tables and columns exist.

```bash
# List all tables
psql -d odoo_payments -c "\dt"

# Expected tables:
# - orders
# - employer_references
# - order_candidates
# - order_odoo_documents
# - payment_transactions
# - webhook_events
# - flyway_schema_history

# Check orders table structure
psql -d odoo_payments -c "\d orders"

# Should show new columns:
# - employer_id
# - employer_email
# - package_type
# - reference_code
```

---

### Test 3: Employer Reference Lookup

**Purpose**: Verify employer can be found by reference code.

**Option A: Via psql**
```sql
SELECT * FROM employer_references WHERE reference_code = 'TEST-REF-001';

-- Expected: 1 row with employer_id = 'EMP-001'
```

**Option B: Via Java test (create this)**
```java
@Test
public void testEmployerReferenceLookup() {
    EmployerReference ref = EmployerReference.findByReferenceCode("TEST-REF-001");
    assertNotNull(ref);
    assertEquals("EMP-001", ref.getEmployerId());
    assertEquals("test@employer.com", ref.getEmployerEmail());
}
```

---

### Test 4: Token Generation (Unit Test)

**Purpose**: Verify JWT token is generated with correct claims.

**Create**: `src/test/java/solutions/envision/odoo/document/service/DocumentTokenServiceTest.java`

```java
@QuarkusTest
class DocumentTokenServiceTest {
    
    @Inject
    DocumentTokenService tokenService;
    
    @Test
    void testGenerateAccessToken_Success() {
        // Given
        Order order = Order.builder()
            .id(UUID.randomUUID())
            .employerId("EMP-001")
            .employerEmail("test@employer.com")
            .packageType(PackageType.PREMIUM)
            .candidateIds(List.of("CAND-001", "CAND-002"))
            .odooDocumentIds(List.of("odoo_doc_001", "odoo_doc_002"))
            .merchantTransactionId("ORD-123")
            .build();
        
        // When
        String token = tokenService.generateAccessToken(order);
        
        // Then
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3); // Valid JWT format
        
        // Decode and verify claims (requires JWT parser)
        // JsonWebToken jwt = jwtParser.parse(token);
        // assertEquals("EMP-001", jwt.getClaim("employerId"));
        // assertEquals("PREMIUM", jwt.getClaim("packageType"));
    }
    
    @Test
    void testGenerateAccessToken_MissingEmployerId_ThrowsException() {
        // Given
        Order order = Order.builder()
            .id(UUID.randomUUID())
            // Missing employerId
            .employerEmail("test@employer.com")
            .packageType(PackageType.PREMIUM)
            .candidateIds(List.of("CAND-001"))
            .build();
        
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            tokenService.generateAccessToken(order);
        });
    }
}
```

**Run Test**:
```bash
./mvnw test -Dtest=DocumentTokenServiceTest
```

---

### Test 5: End-to-End Simulation (Manual)

**Purpose**: Simulate complete payment flow and verify token generation.

**Step 1**: Start application
```bash
./mvnw quarkus:dev
```

**Step 2**: Send simulated payment webhook (requires webhook endpoint)
```bash
curl -X POST http://localhost:8080/webhooks/appypay \
  -H "Content-Type: application/json" \
  -d '{
    "id": "test-payment-001",
    "merchantTransactionId": "ORD-TEST-001",
    "status": "Success",
    "amount": 5000.00,
    "currency": "AOA",
    "paymentMethod": "REF",
    "reference": {
      "referenceNumber": "TEST-REF-001",
      "entity": "00123"
    },
    "customer": {
      "name": "Test Customer",
      "email": "customer@test.com",
      "phone": "+244900000000"
    },
    "createdDate": "2026-02-16T10:00:00Z"
  }'
```

**Step 3**: Check logs for token generation
```bash
# In terminal running quarkus:dev, look for:
INFO  [ao.co.opo.pay.ser.PaymentProcessService] Generated access token for order ...
INFO  [ao.co.opo.pay.ser.NotificationService] ===== DOCUMENT ACCESS EMAIL =====
INFO  [ao.co.opo.pay.ser.NotificationService] TO: test@employer.com
INFO  [ao.co.opo.pay.ser.NotificationService] Download Link: http://localhost:8080/api/documents/download?token=eyJhbG...
```

**Step 4**: Verify order in database
```sql
SELECT 
    id, 
    merchant_transaction_id,
    employer_id,
    employer_email,
    package_type,
    status
FROM orders
WHERE merchant_transaction_id = 'ORD-TEST-001';

-- Expected:
-- employer_id: EMP-001
-- package_type: STANDARD
-- status: COMPLETED
```

**Step 5**: Check candidate IDs were stored
```sql
SELECT * FROM order_candidates 
WHERE order_id = (SELECT id FROM orders WHERE merchant_transaction_id = 'ORD-TEST-001');

-- Expected: 2 rows (CAND-001, CAND-002)
```

---

## 🔍 Review Questions & Answers

### Q1: Why is `NotificationService` just logging and not sending emails?

**A**: Phase 1 focuses on critical functionality. Email sending requires:
- SMTP server configuration
- Email templates (HTML/text)
- Delivery verification
- Bounce handling
- Retry logic for failed sends

This will be implemented in Phase 2. For now, logging allows us to:
- ✅ Verify email content is correct
- ✅ Test the integration without external dependencies
- ✅ See what would be sent in production

**Action**: Review logged email content in Test 5 above. Ensure it contains all required information.

---

### Q2: What happens if `enrichOrderWithEmployerInfo()` fails?

**A**: The entire payment processing fails with an exception. This is **intentional** because:
- Without employer info, we can't generate tokens
- Without tokens, employer can't access documents
- Better to fail fast than create incomplete orders

**Error Flow**:
```
enrichOrderWithEmployerInfo() throws exception
    ↓
handleSuccessfulPayment() catches exception
    ↓
WebhookProcessor logs error
    ↓
Payment marked as FAILED (not COMPLETED)
    ↓
Admin notified via logs (Task 4 will add Slack alerts)
```

**Action**: Ensure all payment reference codes exist in `employer_references` table before processing webhooks.

---

### Q3: Why are `extractCandidateIds()` and `mapCandidatesToOdooDocuments()` placeholders?

**A**: We need business clarification on:
1. **How candidate IDs are provided**: Via webhook payload? Pre-stored in order? Parsed from merchantTransactionId?
2. **When documents are mapped**: At payment time? When order is created? On-demand?
3. **What to do if Odoo API fails**: Cache mappings? Fail payment? Retry later?

**Phase 1 Approach**: Use placeholder data so we can:
- ✅ Test token generation logic
- ✅ Verify database schema
- ✅ Validate JWT claims structure

**Phase 2**: Replace with real implementations after business rules are defined.

**Action**: Document expected webhook payload format including candidate IDs.

---

### Q4: Is the JWT token secure?

**A**: Yes, with caveats:

**Security Features** ✅:
- Token is signed with RSA private key (prevents tampering)
- Includes expiration timestamp (not valid forever)
- Includes employer ID (for authorization checks)
- Private key is git-ignored (not exposed)

**Security Limitations** ⚠️:
- No token revocation (can't invalidate before expiration)
- No rate limiting (could download repeatedly)
- No IP restriction (token works from any IP)
- No refresh tokens (must get new token when expires)

**Phase 2 Improvements**:
- Add token blacklist for revocation
- Implement rate limiting (X downloads per hour)
- Track suspicious activity (different IPs, user agents)
- Add refresh token mechanism

**Action**: Accept current implementation for Phase 1. Plan security enhancements for Phase 2.

---

### Q5: What if Odoo is down when processing payment?

**A**: Currently, payment processing will succeed but token generation will fail. Here's why:

**Current Behavior**:
```java
try {
    // Generate token
    String token = tokenService.generateAccessToken(order);
} catch (Exception e) {
    LOG.errorf("Failed to generate access token");
    // ⚠️ Payment is still marked as COMPLETED
    // ⚠️ No token generated
    // ⚠️ Employer cannot access documents
}
```

**Problem**: Employer paid but can't access documents until admin manually generates token.

**Task 4 Solution**: Add retry logic with exponential backoff:
- Retry token generation 5 times
- If still fails, send Slack alert to admin
- Admin can manually generate token later

**Action**: This is acceptable for Phase 1. Task 4 will add proper retry and alerting.

---

## ✅ Review Checklist

Use this checklist to track your review progress:

### Security
- [ ] No secrets in `application.yml`
- [ ] No secrets in Java code
- [ ] `privateKey.pem` is git-ignored
- [ ] `.env` is git-ignored
- [ ] `.env.example` has no real credentials

### Configuration
- [ ] Odoo config uses separate properties (not database props)
- [ ] JWT config includes private key file path
- [ ] Package expiration times are correct
- [ ] Default values are appropriate for development

### Database
- [ ] V2 migration runs successfully
- [ ] All 3 new tables created
- [ ] orders table has 4 new columns
- [ ] All 9 indexes created
- [ ] Test data inserted

### Code Quality
- [ ] All new classes have Javadoc comments
- [ ] Method names are clear and descriptive
- [ ] No hardcoded values (use constants or config)
- [ ] Error messages are helpful
- [ ] Logging is appropriate (INFO for success, ERROR for failures)

### Business Logic
- [ ] Employer lookup works correctly
- [ ] Package types match business requirements
- [ ] Token includes all required claims
- [ ] Token expiration matches package type
- [ ] Placeholder implementations are clearly marked with TODO

### Testing
- [ ] Can run application without errors
- [ ] Database migrations succeed
- [ ] Employer reference lookup works
- [ ] Token generation creates valid JWT
- [ ] (Optional) End-to-end test passes

---

## 🚦 Review Decision

After completing this review, choose one:

### ✅ APPROVE - Ready to Continue
**If**:
- All critical checks pass
- Code quality is acceptable
- No security concerns
- Placeholder implementations are acceptable for Phase 1

**Next Steps**:
- Proceed with Task 4 (Retry Logic)
- Proceed with Task 5 (Access Logging)
- Write comprehensive tests

### ⚠️ APPROVE WITH CHANGES - Minor Issues
**If**:
- Some non-critical issues found
- Documentation needs updates
- Variable naming improvements needed

**Next Steps**:
- Document issues to fix
- Proceed with Tasks 4-5
- Address issues in parallel

### ❌ REJECT - Major Issues Found
**If**:
- Security vulnerabilities discovered
- Database schema has critical flaws
- Business logic is incorrect
- Code quality is unacceptable

**Next Steps**:
- Document all issues
- Fix critical problems first
- Re-review before continuing

---

## 📝 Review Notes Template

Use this template to document your review findings:

```markdown
# Phase 1 Code Review Notes

**Reviewer**: [Your Name]
**Date**: [Date]
**Decision**: ✅ APPROVE / ⚠️ APPROVE WITH CHANGES / ❌ REJECT

## Summary
[1-2 sentences about overall impression]

## Security
- ✅ [What's good]
- ⚠️ [What needs attention]
- ❌ [What's broken]

## Code Quality
- ✅ [What's good]
- ⚠️ [What needs attention]

## Issues Found
1. [Issue description]
   - Severity: Critical / High / Medium / Low
   - File: [filename:line]
   - Fix: [how to fix]

## Questions
1. [Question about implementation]

## Recommendations
1. [Suggestion for improvement]

## Approval Conditions
[If "Approve with Changes", list conditions that must be met]

## Next Steps
[What should happen next]
```

---

## 📞 Support During Review

If you have questions during the review:

1. **Check Documentation First**:
   - `SETUP.md` - Setup instructions
   - `ACTIONABLE_TASKS.md` - Original task descriptions
   - `CODE_ANALYSIS_AND_TEST_STRATEGY.md` - Test strategy

2. **Run Tests**:
   ```bash
   # Verify application starts
   ./mvnw quarkus:dev
   
   # Run database migrations
   ./mvnw flyway:migrate
   
   # Check database
   psql -d odoo_payments -c "\dt"
   ```

3. **Check Logs**:
   - Application logs in terminal running `quarkus:dev`
   - Database logs if migration fails

---

**Review Time Estimate**: 30-45 minutes  
**Next Step**: Complete review, then decide on Tasks 4-5

Good luck with the review! 🚀
