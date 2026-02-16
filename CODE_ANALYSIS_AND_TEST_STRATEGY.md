# Code Analysis & Test Strategy for Odoo-Backend Integration

## Executive Summary

This document provides a comprehensive analysis of the Quarkus-based webhook service integrating AppyPay/MultiCaixa payment processing with Odoo SaaS for document management. The analysis covers requirements compliance, gap identification, and detailed test strategy recommendations.

**System Overview:**
- **Payment Gateway**: AppyPay/MultiCaixa (Angola)
- **Backend Framework**: Quarkus 3.28.4 with Java 25
- **Document Management**: Odoo SaaS via XML-RPC API
- **Database**: PostgreSQL
- **Integration Method**: XML-RPC for Odoo, REST webhooks for AppyPay

---

## 1. Requirements Compliance Matrix

### Payment Flow Requirements

| Requirement | Status | Implementation | Gap/Issue |
|-------------|--------|----------------|-----------|
| **Webhook receives payment confirmation from AppyPay/MultiCaixa** | ✅ | Implemented in `AppyPayWebhookResource` (assumed to exist based on test references) with POST `/webhooks/appypay` endpoint | None |
| **Payment data is validated and processed asynchronously** | ✅ | `WebhookProcessor` uses SmallRye Reactive Messaging with `@Blocking` annotation for async processing | None |
| **Upon successful payment, system generates secure document access tokens** | ⚠️ | `DocumentAccessTokenService.generateToken()` creates JWT tokens with candidate IDs and package types | **GAP**: No automated token generation triggered by successful payment - requires manual integration between `PaymentProcessService` and `DocumentAccessTokenService` |
| **Tokens are stored with expiration timestamps and usage limits** | ✅ | `DocumentAccessTokenEntity` stores tokens with `expiresAt`, `downloadCount`, and `maxDownloads` fields; persisted to database | None |

### Document Access Requirements

| Requirement | Status | Implementation | Gap/Issue |
|-------------|--------|----------------|-----------|
| **Employers can access documents via token-based URLs (not Odoo portal)** | ✅ | `DocumentAccessResource` provides `/api/documents/download/{candidateId}/{documentId}?token=xxx` endpoint | None |
| **System validates tokens before allowing document downloads** | ✅ | `DocumentAccessTokenService.validateToken()` checks token existence, expiration, and download limits | None |
| **Document retrieval uses Odoo JSON-RPC/REST API** | ✅ | `OdooDocumentClient` uses Apache XML-RPC client to communicate with Odoo's `/xmlrpc/2/common` and `/xmlrpc/2/object` endpoints | **MINOR**: Uses XML-RPC not JSON-RPC, but functionally equivalent |
| **Access attempts are logged for audit purposes** | ✅ | `DocumentAccessTokenService.recordAccess()` creates `DocumentAccessAudit` records with timestamp, IP address, candidate ID, and document ID | None |
| **Expired or invalid tokens return appropriate error responses** | ✅ | `TokenValidationResult` returns specific error messages ("Token not found", "Token expired", "Download limit exceeded") with 403 Forbidden status | None |

### Integration Requirements

| Requirement | Status | Implementation | Gap/Issue |
|-------------|--------|----------------|-----------|
| **External mapping between payment records and Odoo document IDs** | ⚠️ | `PaymentTransaction` stores payment details; `DocumentAccessTokenEntity` stores candidate IDs (which map to Odoo applicants) | **GAP**: No explicit mapping table between `payment_transactions` and Odoo document IDs. Candidate IDs are stored in tokens but not linked to payment transactions. |
| **No Odoo Studio customization required** | ✅ | Uses standard Odoo models (`hr.applicant`, `ir.attachment`) via XML-RPC API | None |
| **Proper error handling for Odoo API failures** | ⚠️ | `OdooDocumentClient` has `ensureAuthenticated()` and catches generic `Exception` | **GAP**: No specific retry logic, circuit breaker, or fallback mechanisms. Error messages thrown as generic exceptions. |
| **Secure storage of Odoo credentials** | ⚠️ | Credentials stored in `application.yml` as `quarkus.datasource.username` and `quarkus.datasource.password` (intended for database, incorrectly used for Odoo) | **ISSUE**: Configuration uses database credentials for Odoo authentication. Should use separate `odoo.username` and `odoo.password` properties. **SECURITY RISK**: Credentials in plaintext config files. |
| **Rate limiting and retry logic for Odoo API calls** | ❌ | Not implemented | **MISSING**: No rate limiting, no retry decorator, no exponential backoff for failed Odoo API calls. |

---

## 2. Detailed Gap Analysis

### ⚠️ HIGH PRIORITY GAPS

#### Gap 1: Missing Payment-to-Token Integration
**Issue**: Successful payments do not automatically generate document access tokens.

**Current State**: 
- `PaymentProcessService.handleSuccessfulPayment()` marks order as PAID and sends data to Odoo
- No call to `DocumentAccessTokenService.generateToken()`

**Required Implementation**:
```java
// In PaymentProcessService.handleSuccessfulPayment()
private void handleSuccessfulPayment(AppyPayWebhookPayload payload) {
    // ... existing code ...
    
    // Generate document access token
    String employerId = determineEmployerId(order); // Need to implement
    List<Integer> candidateIds = getCandidateIdsForOrder(order); // Need to implement
    String packageType = determinePackageType(order); // Need to implement
    
    documentAccessTokenService.generateToken(employerId, candidateIds, packageType);
}
```

**Impact**: Core functionality missing - employers cannot access documents after payment.

---

#### Gap 2: Payment-Document Mapping
**Issue**: No explicit mapping between payment transactions and Odoo document/candidate IDs.

**Current State**:
- `Order` has `referenceId` but this links to a payment reference, not Odoo candidates
- No field in `PaymentTransaction` or `Order` to store candidate IDs or job position IDs

**Required Implementation**:
1. Add mapping table:
```sql
CREATE TABLE payment_document_mapping (
    id UUID PRIMARY KEY,
    payment_transaction_id UUID NOT NULL,
    candidate_id INTEGER NOT NULL,
    job_id INTEGER,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (payment_transaction_id) REFERENCES payment_transactions(id)
);
```

2. Or extend `Order` entity:
```java
@ElementCollection
@CollectionTable(name = "order_candidates")
private List<Integer> candidateIds;
```

**Impact**: Cannot determine which candidates' documents should be accessible after payment.

---

#### Gap 3: Incorrect Odoo Credential Configuration
**Issue**: `OdooDocumentClient` uses database credentials for Odoo authentication.

**Current Implementation** (lines 20-26 in OdooDocumentClient.java):
```java
@ConfigProperty(name = "quarkus.datasource.db-kind")
String database;  // This is "postgresql", not Odoo database name!

@ConfigProperty(name = "quarkus.datasource.username")
String username;  // Database username, not Odoo username

@ConfigProperty(name = "quarkus.datasource.password")
String password;  // Database password, not Odoo password
```

**Required Fix**:
```java
@ConfigProperty(name = "odoo.database")
String database;

@ConfigProperty(name = "odoo.username")
String username;

@ConfigProperty(name = "odoo.password")
String password;
```

**Update `application.yml`**:
```yaml
odoo:
  url: ${ODOO_URL:http://localhost:8069}
  database: ${ODOO_DATABASE:odoo_production}
  username: ${ODOO_USERNAME:admin}
  password: ${ODOO_PASSWORD:changeme}
```

**Impact**: Authentication will fail with Odoo. Critical bug.

---

#### Gap 4: No Error Recovery for Odoo Failures
**Issue**: No retry logic, circuit breaker, or fallback mechanisms for Odoo API calls.

**Current State**: Methods throw generic `Exception` on any failure.

**Required Implementation**:
```java
@ApplicationScoped
public class OdooDocumentClient {
    
    @Retry(maxRetries = 3, delay = 1000, jitter = 500)
    @Timeout(value = 10, unit = ChronoUnit.SECONDS)
    @Fallback(fallbackMethod = "getCandidateDocumentsFallback")
    public List<OdooDocument> getCandidateDocuments(Integer candidateId) throws Exception {
        // existing implementation
    }
    
    public List<OdooDocument> getCandidateDocumentsFallback(Integer candidateId) {
        LOG.warn("Odoo API unavailable, returning cached/empty result");
        return Collections.emptyList();
    }
}
```

**Dependencies Needed**:
```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-smallrye-fault-tolerance</artifactId>
</dependency>
```

**Impact**: System fragile to Odoo downtime or network issues.

---

### ⚠️ MEDIUM PRIORITY GAPS

#### Gap 5: No Rate Limiting for Odoo API Calls
**Issue**: Can overwhelm Odoo API with requests.

**Required Implementation**: Add rate limiter using Resilience4j or Bucket4j:
```java
private RateLimiter rateLimiter = RateLimiter.of("odoo-api", 
    RateLimiterConfig.custom()
        .limitForPeriod(50)
        .limitRefreshPeriod(Duration.ofSeconds(1))
        .build()
);

public List<OdooDocument> getCandidateDocuments(Integer candidateId) {
    rateLimiter.acquirePermission();
    // ... existing code
}
```

---

#### Gap 6: Missing JWT Secret Configuration
**Issue**: `DocumentAccessTokenService` uses `@ConfigProperty(name = "jwt.secret")` but this is not defined in `application.yml`.

**Current State**: Missing configuration will cause application startup failure.

**Required Addition to `application.yml`**:
```yaml
jwt:
  secret: ${JWT_SECRET:please-change-this-secret-in-production}
  issuer: document-service
```

**Impact**: Token generation will fail.

---

#### Gap 7: Incomplete Access Logging
**Issue**: `DocumentAccessLog` entity exists but is only used for single-document tokens, not for package tokens.

**Current Behavior**: When downloading via package token, only `DocumentAccessAudit` is recorded, not `DocumentAccessLog`.

**Recommendation**: Clarify purpose of two separate entities or consolidate into one audit table.

---

### 🔍 CLARIFICATIONS NEEDED

#### Clarification 1: How to Determine Employer ID from Payment
**Question**: How does the system know which employer ID to associate with a payment?

**Current State**: `Order` has customer info but no employer ID field.

**Options**:
1. Add `employerId` field to `Order` entity
2. Extract from `merchantTransactionId` using pattern
3. Link via separate employer-reference mapping table

**Decision Required**: Which approach should be used?

---

#### Clarification 2: When to Generate Tokens
**Question**: Should tokens be generated:
- A) Automatically on successful payment?
- B) On-demand when employer requests access?
- C) Via separate admin API call?

**Current Implementation**: Only option C is possible (no automation).

**Decision Required**: Clarify token generation trigger.

---

#### Clarification 3: Token Scope Definition
**Question**: How are candidate IDs determined for a payment/token?

**Options**:
1. Employer pays for access to specific job posting → all candidates for that job
2. Employer pays for individual candidate access
3. Employer pays for package (basic/standard/premium) → determines candidate count, but which candidates?

**Current State**: `generateToken()` accepts `List<Integer> candidateIds` but no clear source for this data.

**Decision Required**: Define business logic for candidate selection.

---

## 3. Missing Integration Tests

### Category A: Odoo API Connection Tests

#### ❌ Test: `testOdooAuthentication_Success`
**Purpose**: Verify successful authentication with Odoo  
**Setup**: Mock or test Odoo instance with valid credentials  
**Method Under Test**: `OdooDocumentClient.init()`  
**Assertions**:
- `uid` is not null
- `modelsClient` is initialized
- Can make subsequent authenticated calls

**Current Status**: Not implemented

---

#### ❌ Test: `testOdooAuthentication_InvalidCredentials`
**Purpose**: Handle authentication failures gracefully  
**Setup**: Mock Odoo returning authentication error  
**Method Under Test**: `OdooDocumentClient.init()`  
**Assertions**:
- Throws `OdooAuthenticationException`
- Error message is descriptive
- Application doesn't crash

**Current Status**: Not implemented

---

#### ❌ Test: `testOdooAuthentication_NetworkTimeout`
**Purpose**: Handle network timeouts during authentication  
**Setup**: Mock Odoo with delayed response (>10s)  
**Method Under Test**: `OdooDocumentClient.init()`  
**Assertions**:
- Throws `TimeoutException` or similar
- Timeout is configurable
- Subsequent calls can retry

**Current Status**: Not implemented

---

#### ❌ Test: `testOdooAuthentication_RateLimiting`
**Purpose**: Verify rate limiting behavior  
**Setup**: Make 100 rapid authentication attempts  
**Method Under Test**: `OdooDocumentClient.init()`  
**Assertions**:
- Rate limiter prevents excess requests
- Appropriate backpressure applied
- No Odoo API overload

**Current Status**: Not implemented (feature not implemented)

---

### Category B: Document Retrieval Tests

#### ❌ Test: `testGetCandidateDocuments_Success`
**Purpose**: Fetch candidate documents successfully  
**Setup**: Mock Odoo returning 3 PDF documents  
**Method Under Test**: `OdooDocumentClient.getCandidateDocuments(123)`  
**Assertions**:
- Returns list of 3 `OdooDocument` objects
- Each document has valid id, name, datas (Base64), mimetype, fileSize
- MIME types are correct (application/pdf)

**Current Status**: Not implemented

---

#### ❌ Test: `testGetCandidateDocuments_NonExistentCandidate`
**Purpose**: Handle requests for non-existent candidates  
**Setup**: Mock Odoo returning empty result set  
**Method Under Test**: `OdooDocumentClient.getCandidateDocuments(999999)`  
**Assertions**:
- Returns empty list (not null)
- No exception thrown
- Logs warning about missing candidate

**Current Status**: Not implemented

---

#### ❌ Test: `testGetCandidateDocuments_OdooApiError`
**Purpose**: Handle Odoo API errors during document fetch  
**Setup**: Mock Odoo returning HTTP 500 or XML-RPC fault  
**Method Under Test**: `OdooDocumentClient.getCandidateDocuments(123)`  
**Assertions**:
- Throws `OdooApiException` with descriptive message
- Original error is logged
- Retry logic is triggered (if implemented)

**Current Status**: Not implemented

---

#### ❌ Test: `testDownloadDocument_Success`
**Purpose**: Download binary document successfully  
**Setup**: Mock Odoo returning Base64-encoded PDF  
**Method Under Test**: `OdooDocumentClient.downloadDocument(456)`  
**Assertions**:
- Returns `byte[]` with correct length
- Decoded content is valid PDF
- No data corruption

**Current Status**: Not implemented

---

#### ❌ Test: `testDownloadDocument_LargeFile`
**Purpose**: Handle large document downloads  
**Setup**: Mock Odoo returning 50MB document  
**Method Under Test**: `OdooDocumentClient.downloadDocument(789)`  
**Assertions**:
- Successfully downloads large file
- No memory overflow
- Reasonable timeout (configurable)

**Current Status**: Not implemented

---

#### ❌ Test: `testDownloadDocument_CorruptedData`
**Purpose**: Handle corrupted Base64 data from Odoo  
**Setup**: Mock Odoo returning invalid Base64 string  
**Method Under Test**: `OdooDocumentClient.downloadDocument(456)`  
**Assertions**:
- Throws `DocumentCorruptedException`
- Error is logged with document ID
- Client can recover

**Current Status**: Not implemented

---

### Category C: End-to-End Payment-to-Document Flow Tests

#### ❌ Test: `testE2E_PaymentToDocumentAccess_Success`
**Purpose**: Verify complete flow from payment webhook to document download  
**Setup**: 
1. Mock Odoo with candidate documents
2. Post payment webhook with SUCCESS status
3. Generate access token (after implementing integration)
4. Download document using token

**Steps**:
```java
// 1. Receive payment webhook
POST /webhooks/appypay
{
  "id": "PAY-123",
  "merchantTransactionId": "ORD-456",
  "status": "Success",
  "amount": 5000.00,
  "customer": {...}
}

// 2. Verify payment processed
GET /api/orders/ORD-456
→ status: PAID

// 3. Verify token generated (after implementing)
GET /api/tokens?orderId=...
→ token: "eyJhbGc..."

// 4. Download document
GET /api/documents/download/123/456?token=eyJhbGc...
→ 200 OK, Content-Type: application/pdf
```

**Assertions**:
- Payment webhook returns 200 immediately
- Async processing completes within 5 seconds
- Token is valid and not expired
- Document download succeeds
- Access audit record created

**Current Status**: Cannot implement due to Gap 1 (missing payment-token integration)

---

#### ❌ Test: `testE2E_PaymentToDocumentAccess_ExpiredToken`
**Purpose**: Verify expired tokens are rejected  
**Setup**: Same as above but manually expire token  
**Assertions**:
- Document download returns 403 Forbidden
- Error message: "Token expired"
- No document data returned

**Current Status**: Partially testable (token validation works, but no E2E flow)

---

#### ❌ Test: `testE2E_PaymentToDocumentAccess_DownloadLimitExceeded`
**Purpose**: Verify download limit enforcement  
**Setup**: 
1. Generate token with basic package (10 downloads)
2. Download 10 documents
3. Attempt 11th download

**Assertions**:
- First 10 downloads succeed
- 11th download returns 403 Forbidden
- Error message: "Download limit exceeded"
- Download count correctly tracked

**Current Status**: Partially testable (limit validation works, but no E2E flow)

---

### Category D: Token-Document Mapping Tests

#### ❌ Test: `testTokenMapping_MultipleTokensSameDocument`
**Purpose**: Verify multiple employers can access same document via different tokens  
**Setup**: 
1. Create token for Employer A with candidate 123
2. Create token for Employer B with candidate 123
3. Both download same document

**Assertions**:
- Both tokens are valid
- Both can download document
- Access audits show separate records for each employer

**Current Status**: Should work but not tested

---

#### ❌ Test: `testTokenMapping_InvalidCandidateId`
**Purpose**: Handle tokens with non-existent candidate IDs  
**Setup**: Create token with candidateId=999999 (doesn't exist in Odoo)  
**Assertions**:
- Token creation succeeds
- Document list request returns empty or error
- Download request returns 404 Not Found

**Current Status**: Not implemented

---

### Category E: Error Recovery Tests

#### ❌ Test: `testErrorRecovery_OdooTemporarilyUnavailable`
**Purpose**: Handle transient Odoo outages  
**Setup**: 
1. Mock Odoo to fail first 2 requests, succeed on 3rd
2. Implement retry logic with exponential backoff

**Method Under Test**: `OdooDocumentClient.getCandidateDocuments(123)`  
**Assertions**:
- First 2 attempts fail
- 3rd attempt succeeds after retry
- Total time includes backoff delays
- Success is logged

**Current Status**: Not implemented (retry logic missing)

---

#### ❌ Test: `testErrorRecovery_DocumentDeletedFromOdoo`
**Purpose**: Handle documents deleted after token generation  
**Setup**: 
1. Create token with documentId=456
2. Mock Odoo to return 404 for document 456

**Assertions**:
- Download request returns 404 Not Found
- Error message: "Document not found in Odoo"
- Token remains valid (not corrupted by error)

**Current Status**: Not tested

---

## 4. Missing Unit Tests

### Class: `OdooDocumentClient`

#### ❌ Test: `testGetCandidateInfo_Success`
**Mock**: XML-RPC client returning candidate data  
**Scenario**: Valid candidate ID  
**Expected**: `CandidateInfo` object with name and email  
**Assertions**:
```java
CandidateInfo info = client.getCandidateInfo(123);
assertNotNull(info);
assertEquals(123, info.id());
assertEquals("John Doe", info.name());
assertEquals("john@example.com", info.email());
```

---

#### ❌ Test: `testGetCandidateInfo_NotFound`
**Mock**: XML-RPC returning empty array  
**Scenario**: Non-existent candidate ID  
**Expected**: Throw `IllegalArgumentException`  
**Assertions**:
```java
assertThrows(IllegalArgumentException.class, () -> {
    client.getCandidateInfo(999999);
});
```

---

#### ❌ Test: `testGetCandidatesByJob_Success`
**Mock**: XML-RPC returning array of candidate IDs  
**Scenario**: Valid job ID with 5 candidates  
**Expected**: List of 5 integers  

---

#### ❌ Test: `testGetCandidatesByJob_EmptyJob`
**Mock**: XML-RPC returning empty array  
**Scenario**: Job with no candidates  
**Expected**: Empty list (not null)  

---

#### ❌ Test: `testEnsureAuthenticated_ReauthenticationOnExpiry`
**Mock**: First call has `uid=null`, subsequent calls have `uid=12345`  
**Scenario**: Session expired, needs re-authentication  
**Expected**: Automatic re-authentication without error  

---

### Class: `DocumentAccessTokenService`

#### ❌ Test: `testGenerateToken_WithNullEmployerId`
**Scenario**: Call with `employerId=null`  
**Expected**: Throw `IllegalArgumentException` or validation error  

---

#### ❌ Test: `testGenerateToken_WithEmptyCandidateIds`
**Scenario**: Call with empty candidate list  
**Expected**: Throw validation error or create token with no access  

---

#### ❌ Test: `testValidateToken_ConcurrentAccess`
**Scenario**: 10 threads validate same token simultaneously  
**Expected**: Download count incremented correctly (no race conditions)  
**Setup**: Use `CountDownLatch` to synchronize threads  

---

#### ❌ Test: `testRecordAccess_NullIpAddress`
**Scenario**: Call with `ipAddress=null`  
**Expected**: Audit record created with null IP (don't fail)  

---

### Class: `PaymentProcessService`

#### ❌ Test: `testHandleSuccessfulPayment_GeneratesToken`
**Mock**: `DocumentAccessTokenService`, `OdooPaymentService`, `OrderService`  
**Scenario**: Successful payment webhook received  
**Expected**: Token generation method called with correct parameters  
**Current Status**: Test needed after implementing Gap 1  

---

#### ❌ Test: `testHandleSuccessfulPayment_OdooNotificationFails`
**Mock**: `OdooPaymentService.sendPaymentToOdoo()` throws exception  
**Scenario**: Payment processed but Odoo notification fails  
**Expected**: Payment still marked as PAID, error logged, token still generated  

---

#### ❌ Test: `testProcessPaymentStatus_UnknownStatus`
**Scenario**: Webhook with status="REFUNDED" (not in switch statement)  
**Expected**: Logs warning, doesn't throw exception  

---

### Class: `DocumentAccessResource`

#### ❌ Test: `testGetAccessibleCandidates_OdooDown`
**Mock**: `OdooDocumentClient.getCandidateInfo()` throws exception  
**Scenario**: Odoo API is down  
**Expected**: Return 500 with error message, partial results if some candidates succeeded  

---

#### ❌ Test: `testDownloadDocument_CandidateNotInToken`
**Scenario**: Request document for candidateId=999 but token only has [1,2,3]  
**Expected**: Return 403 Forbidden  

---

#### ❌ Test: `testDownloadDocument_VerifyContentDisposition`
**Scenario**: Download document with name "Resume João Silva.pdf"  
**Expected**: Content-Disposition header has correct filename with proper encoding  

---

#### ❌ Test: `testGenerateDownloadLink_ExhaustedMasterToken`
**Scenario**: Master token already used max downloads  
**Expected**: Return 403, cannot generate new single-use links  

---

### Class: `WebhookProcessor`

#### ❌ Test: `testProcessPayment_ConcurrentWebhooks`
**Scenario**: 100 webhooks received simultaneously  
**Expected**: All processed correctly, no deadlocks, correct order status  

---

#### ❌ Test: `testProcessPayment_RetriesAfterTransientFailure`
**Mock**: `PaymentProcessService` fails first 2 calls, succeeds on 3rd  
**Scenario**: Database temporarily unavailable  
**Expected**: Retry logic succeeds, webhook marked as processed  
**Current Status**: No retry in WebhookProcessor (only marks as failed)  

---

## 5. Test Infrastructure Recommendations

### Required Setup

#### TestContainers Configuration
```java
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
public class DocumentAccessIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("test_db")
        .withUsername("test")
        .withPassword("test");
    
    @BeforeAll
    static void setup() {
        postgres.start();
        System.setProperty("quarkus.datasource.jdbc.url", postgres.getJdbcUrl());
    }
}
```

**Dependencies**:
```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.19.8</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <version>1.19.8</version>
    <scope>test</scope>
</dependency>
```

---

#### Mock Odoo Server Strategy

**Option A: WireMock (Recommended for integration tests)**
```java
@QuarkusTest
@TestProfile(OdooMockProfile.class)
public class OdooDocumentClientIntegrationTest {
    
    static WireMockServer wireMock = new WireMockServer(8069);
    
    @BeforeAll
    static void setupWireMock() {
        wireMock.start();
        
        // Mock authentication
        wireMock.stubFor(post("/xmlrpc/2/common")
            .willReturn(ok().withBody("<?xml version='1.0'?><methodResponse>..." +
                "<int>12345</int>...</methodResponse>")));
        
        // Mock document fetch
        wireMock.stubFor(post("/xmlrpc/2/object")
            .willReturn(ok().withBody("... candidate documents XML-RPC response ...")));
    }
}
```

**Option B: Real Odoo Test Instance (Recommended for E2E tests)**
- Spin up Odoo in Docker Compose with test data
- Use Odoo's demo data or custom fixtures
- Reset database between test runs

**Docker Compose Example**:
```yaml
services:
  odoo-test:
    image: odoo:16.0
    ports:
      - "8069:8069"
    environment:
      - HOST=postgres-test
      - USER=odoo
      - PASSWORD=odoo
    depends_on:
      - postgres-test
  
  postgres-test:
    image: postgres:15
    environment:
      - POSTGRES_USER=odoo
      - POSTGRES_PASSWORD=odoo
      - POSTGRES_DB=odoo_test
```

---

### Test Data Fixtures

#### Fixture: Valid Payment Webhook
```java
public class TestFixtures {
    
    public static AppyPayWebhookPayload validSuccessPayment() {
        return AppyPayWebhookPayload.builder()
            .id("PAY-TEST-001")
            .merchantTransactionId("ORD-TEST-001")
            .amount(new BigDecimal("5000.00"))
            .currency("AOA")
            .status("Success")
            .paymentMethod("REF")
            .reference(ReferenceInfo.builder()
                .referenceNumber("123456789")
                .entity("00123")
                .dueDate(Instant.now().plus(30, ChronoUnit.DAYS))
                .build())
            .customer(CustomerInfo.builder()
                .name("Test Customer")
                .email("test@example.com")
                .phone("+244900000000")
                .build())
            .createdDate(Instant.now())
            .updatedDate(Instant.now())
            .build();
    }
    
    public static List<OdooDocument> sampleCandidateDocuments() {
        return List.of(
            new OdooDocument(1, "Resume.pdf", "JVBERi0xLjQK...", "application/pdf", 102400),
            new OdooDocument(2, "Cover Letter.pdf", "JVBERi0xLjQK...", "application/pdf", 51200),
            new OdooDocument(3, "Certificates.pdf", "JVBERi0xLjQK...", "application/pdf", 204800)
        );
    }
}
```

---

### Test Profiles

#### Profile: Integration Test with Mock Odoo
```java
public class IntegrationTestProfile implements QuarkusTestProfile {
    
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
            "odoo.webhook.url", "http://localhost:8069",
            "odoo.database", "test_db",
            "odoo.username", "admin",
            "odoo.password", "admin",
            "jwt.secret", "test-secret-key-for-jwt",
            "quarkus.datasource.jdbc.url", "jdbc:tc:postgresql:15-alpine:///test_db"
        );
    }
    
    @Override
    public Set<Class<?>> getEnabledAlternatives() {
        return Set.of(MockOdooDocumentClient.class);
    }
}
```

---

### CI/CD Integration

#### GitHub Actions Example
```yaml
name: Integration Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
          POSTGRES_DB: test_db
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
      
      odoo:
        image: odoo:16.0
        env:
          HOST: postgres
          USER: odoo
          PASSWORD: odoo
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Run tests
        run: ./mvnw verify
        env:
          DB_URL: jdbc:postgresql://localhost:5432/test_db
          DB_USERNAME: test
          DB_PASSWORD: test
          ODOO_URL: http://localhost:8069
          ODOO_DATABASE: odoo
          ODOO_USERNAME: admin
          ODOO_PASSWORD: admin
      
      - name: Upload coverage
        uses: codecov/codecov-action@v3
```

---

### Assertion Patterns for Async Operations

#### Pattern 1: Awaitility for Async Verification
```java
@Test
public void testWebhookProcessedAsynchronously() {
    // Send webhook
    given()
        .contentType(ContentType.JSON)
        .body(webhookPayload)
    .when()
        .post("/webhooks/appypay")
    .then()
        .statusCode(200);
    
    // Wait for async processing
    await().atMost(10, SECONDS)
        .pollInterval(500, MILLISECONDS)
        .untilAsserted(() -> {
            WebhookEventEntity event = webhookRepository.findByAppyPayTxId(webhookPayload.getId());
            assertEquals("PROCESSED", event.getProcessingStatus());
        });
}
```

**Dependency**:
```xml
<dependency>
    <groupId>org.awaitility</groupId>
    <artifactId>awaitility</artifactId>
    <version>4.2.0</version>
    <scope>test</scope>
</dependency>
```

#### Pattern 2: @QuarkusTest with @TestReactive
```java
@Test
@TestReactive
public void testAsyncTokenGeneration() {
    return documentService.generateTokenAsync(employerId, candidateIds, packageType)
        .onItem().transform(token -> {
            assertNotNull(token);
            return token;
        })
        .onItem().invoke(token -> {
            // Additional assertions
        })
        .await().atMost(Duration.ofSeconds(5));
}
```

---

## 6. Test Execution Strategy

### Fast Feedback Strategy

#### Unit Tests (< 5 seconds)
```bash
./mvnw test -Dtest=*Test
```
- Mock all external dependencies
- Use in-memory H2 database
- Run in parallel with `<forkCount>4</forkCount>`

#### Integration Tests (< 2 minutes)
```bash
./mvnw verify -Dtest=*IT
```
- Use TestContainers for PostgreSQL
- Use WireMock for Odoo
- Run sequentially for stability

#### E2E Tests (< 10 minutes)
```bash
./mvnw verify -P e2e-tests
```
- Use real Odoo Docker instance
- Full database setup
- Run on CI only, not locally

---

### Test Categorization with JUnit Tags
```java
@Tag("unit")
public class DocumentAccessTokenServiceTest { }

@Tag("integration")
public class OdooDocumentClientIntegrationTest { }

@Tag("e2e")
public class FullPaymentFlowTest { }
```

**Run specific category**:
```bash
./mvnw test -Dgroups=unit
./mvnw test -Dgroups=integration
./mvnw test -Dgroups=e2e
```

---

## 7. Priority Recommendations

### 🔴 HIGH PRIORITY (Implement First)

1. **Fix Odoo Credential Configuration** (Gap 3)
   - Impact: System currently broken for Odoo authentication
   - Effort: 1 hour
   - Dependencies: None

2. **Implement Payment-to-Token Integration** (Gap 1)
   - Impact: Core functionality missing
   - Effort: 1-2 days
   - Dependencies: Clarification 2 and 3

3. **Create Payment-Document Mapping** (Gap 2)
   - Impact: Cannot determine document access
   - Effort: 1 day
   - Dependencies: Business requirements

4. **Add Unit Tests for `OdooDocumentClient`**
   - Coverage: 0% → 80%+
   - Effort: 2-3 days
   - Dependencies: None

5. **Add Integration Test for Odoo Connection**
   - Tests: `testOdooAuthentication_Success`, `testGetCandidateDocuments_Success`
   - Effort: 1 day
   - Dependencies: WireMock setup

---

### 🟡 MEDIUM PRIORITY

6. **Implement Retry Logic with Fault Tolerance** (Gap 4)
   - Impact: System resilience
   - Effort: 1 day
   - Dependencies: Add `quarkus-smallrye-fault-tolerance` dependency

7. **Add Rate Limiting** (Gap 5)
   - Impact: Prevents Odoo API overload
   - Effort: 1 day
   - Dependencies: Decide on rate limit values

8. **Configure JWT Secret** (Gap 6)
   - Impact: Token generation will fail
   - Effort: 30 minutes
   - Dependencies: None

9. **E2E Test: Payment to Document Access**
   - Coverage: End-to-end flow validation
   - Effort: 2 days
   - Dependencies: Gaps 1, 2, 3 resolved

10. **Add TestContainers Infrastructure**
    - Coverage: Realistic database testing
    - Effort: 1 day
    - Dependencies: None

---

### 🟢 LOW PRIORITY (Nice to Have)

11. **Consolidate Access Logging** (Gap 7)
    - Impact: Cleaner data model
    - Effort: 1 day
    - Dependencies: Business decision

12. **Add Performance Tests**
    - Load testing with 1000 concurrent requests
    - Effort: 2-3 days
    - Dependencies: All integration tests passing

13. **Add Security Tests**
    - Token manipulation attempts
    - SQL injection tests
    - Effort: 2 days
    - Dependencies: None

---

## 8. Test Coverage Goals

| Component | Current Coverage | Target Coverage | Priority |
|-----------|------------------|----------------|----------|
| `OdooDocumentClient` | 0% | 85% | High |
| `DocumentAccessTokenService` | 60% (from existing test) | 90% | Medium |
| `PaymentProcessService` | 50% (from existing test) | 80% | High |
| `DocumentAccessResource` | 40% (from existing test) | 75% | Medium |
| `WebhookProcessor` | 0% | 70% | Medium |
| Integration Tests | 20% (only OdooApiClient) | 60% | High |
| E2E Tests | 0% | 30% | Low |

**Overall Target**: 75% code coverage, 90% critical path coverage

---

## 9. Risk Assessment

### High Risk Areas

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| **Odoo API downtime during payment processing** | High | Critical | Implement retry logic, circuit breaker, async queue with DLQ |
| **Incorrect credential configuration breaks production** | Medium | Critical | Add startup validation, integration tests, environment-specific configs |
| **Token generation not triggered after payment** | High (currently missing) | Critical | Implement Gap 1, add E2E test |
| **Document ID mapping missing or incorrect** | High (currently unclear) | Critical | Implement Gap 2, clarify business rules |
| **Rate limiting not implemented** | Medium | Medium | Implement rate limiter, monitor Odoo API usage |

---

## 10. Conclusion

### Summary of Findings

**Strengths**:
- ✅ Solid webhook processing architecture with async handling
- ✅ Token generation and validation logic implemented
- ✅ Good separation of concerns (DDD-style)
- ✅ Some test coverage exists

**Critical Issues**:
- ❌ Odoo credentials misconfigured (uses DB credentials)
- ❌ No integration between payment success and token generation
- ❌ Missing payment-to-document mapping
- ❌ No error recovery for Odoo failures
- ❌ Insufficient integration test coverage

**Recommended Next Steps**:
1. Fix Odoo credential configuration immediately
2. Clarify business requirements (Clarifications 1-3)
3. Implement payment-to-token integration
4. Add comprehensive test suite following this strategy
5. Implement fault tolerance and rate limiting
6. Conduct full E2E testing before production deployment

**Estimated Total Effort**: 15-20 working days for full implementation and testing

---

## Appendix A: Test Checklist

### Pre-Deployment Test Checklist

- [ ] All unit tests passing (>80% coverage)
- [ ] Integration tests for Odoo API passing
- [ ] E2E test: Payment → Token → Document Download
- [ ] Test expired token rejection
- [ ] Test download limit enforcement
- [ ] Test Odoo API failure recovery
- [ ] Test concurrent webhook processing
- [ ] Security test: Token manipulation attempts
- [ ] Load test: 100 concurrent downloads
- [ ] Test with real Odoo instance (staging)
- [ ] Verify access audit logging
- [ ] Test credential configuration per environment
- [ ] Verify JWT secret configured securely
- [ ] Test rate limiting behavior
- [ ] Verify database migrations applied

---

**Document Version**: 1.0  
**Last Updated**: 2026-02-16  
**Status**: Ready for Review
