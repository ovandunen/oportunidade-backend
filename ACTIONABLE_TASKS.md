# Actionable Tasks - Odoo Integration Completion

## Quick Start Priority List

### 🔴 CRITICAL - Must Fix Before Testing

#### Task 1: Fix Odoo Authentication Configuration
**File**: `OdooDocumentClient.java`  
**Effort**: 1 hour  
**Status**: ❌ Blocking

**Problem**: Using database credentials instead of Odoo credentials.

**Changes Required**:

1. **Update `OdooDocumentClient.java` (lines 20-26)**:
```java
// BEFORE (INCORRECT):
@ConfigProperty(name = "quarkus.datasource.db-kind")
String database;

@ConfigProperty(name = "quarkus.datasource.username")
String username;

@ConfigProperty(name = "quarkus.datasource.password")
String password;

// AFTER (CORRECT):
@ConfigProperty(name = "odoo.database")
String database;

@ConfigProperty(name = "odoo.username")
String username;

@ConfigProperty(name = "odoo.password")
String password;
```

2. **Update `application.yml`** - Add new section:
```yaml
odoo:
  url: ${ODOO_URL:http://localhost:8069}
  database: ${ODOO_DATABASE:odoo}
  username: ${ODOO_USERNAME:admin}
  password: ${ODOO_PASSWORD}
  webhook:
    key: ${ODOO_WEBHOOK_KEY}
    url: ${ODOO_WEBHOOK_URL}
```

3. **Add JWT Configuration** (currently missing):
```yaml
jwt:
  secret: ${JWT_SECRET:please-change-this-in-production}
  issuer: oportunidade-document-service
  expiration-hours: 72
```

**Test**: Run `OdooDocumentClient.init()` and verify authentication succeeds.

---

#### Task 2: Add Payment-to-Document Mapping
**Files**: Create new entity + migration  
**Effort**: 4 hours  
**Status**: ❌ Blocking

**Problem**: No link between payments and Odoo candidate/document IDs.

**Option A: Add to Order Entity** (Recommended - simpler):

1. **Update `Order.java`**:
```java
@ElementCollection
@CollectionTable(name = "order_candidate_mapping", 
    joinColumns = @JoinColumn(name = "order_id"))
@Column(name = "candidate_id")
private List<Integer> candidateIds;

@Column(name = "job_id")
private Integer jobId;

@Column(name = "employer_id")
private String employerId;

@Column(name = "package_type")
private String packageType;
```

2. **Create Migration `V2__add_order_document_mapping.sql`**:
```sql
CREATE TABLE order_candidate_mapping (
    order_id UUID NOT NULL,
    candidate_id INTEGER NOT NULL,
    PRIMARY KEY (order_id, candidate_id),
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

ALTER TABLE orders ADD COLUMN job_id INTEGER;
ALTER TABLE orders ADD COLUMN employer_id VARCHAR(255);
ALTER TABLE orders ADD COLUMN package_type VARCHAR(50);
```

3. **Update `OrderEntity.java`** (persistence layer):
```java
@ElementCollection
@CollectionTable(name = "order_candidate_mapping")
private List<Integer> candidateIds;

@Column(name = "job_id")
private Integer jobId;

@Column(name = "employer_id")
private String employerId;

@Column(name = "package_type")
private String packageType;
```

**Test**: Create order with candidate IDs, verify persisted and retrievable.

---

#### Task 3: Implement Payment → Token Generation
**File**: `PaymentProcessService.java`  
**Effort**: 2 hours  
**Status**: ❌ Blocking

**Problem**: Successful payments don't generate document access tokens.

**Implementation**:

1. **Inject `DocumentAccessTokenService`** into `PaymentProcessService`:
```java
@ApplicationScoped
public class PaymentProcessService extends ... {
    
    private final OdooPaymentService odooPaymentService;
    private final DocumentAccessTokenService tokenService; // ADD THIS
    
    public PaymentProcessService(
            OdooPaymentService odooPaymentService,
            DocumentAccessTokenService tokenService) { // ADD THIS
        this.odooPaymentService = odooPaymentService;
        this.tokenService = tokenService; // ADD THIS
    }
}
```

2. **Update `handleSuccessfulPayment()` method**:
```java
private void handleSuccessfulPayment(final AppyPayWebhookPayload payload) {
    LOG.infof("Handling successful payment: %s", payload.getId());

    final OrderService orderService = getSupportingDomainService();
    final Order order = orderService.find(payload);
    order.setStatus(Order.OrderStatus.PAID);
    orderService.transact(order);

    final PaymentTransaction paymentTransaction = createPaymentTransaction(
        payload, order, PaymentTransaction.TransactionStatus.SUCCESS);
    odooPaymentService.sendPaymentToOdoo(paymentTransaction);

    // ===== ADD TOKEN GENERATION HERE =====
    if (order.getEmployerId() != null && 
        order.getCandidateIds() != null && 
        !order.getCandidateIds().isEmpty()) {
        
        try {
            DocumentAccessTokenEntity token = tokenService.generateToken(
                order.getEmployerId(),
                order.getCandidateIds(),
                order.getPackageType() != null ? order.getPackageType() : "standard"
            );
            
            LOG.infof("Generated document access token for employer %s: %s", 
                order.getEmployerId(), token.getId());
            
            // TODO: Send token to employer via email/webhook
            
        } catch (Exception e) {
            LOG.errorf(e, "Failed to generate access token for order: %s", 
                order.getMerchantTransactionId());
            // Don't fail the payment, but log the error
        }
    } else {
        LOG.warnf("Cannot generate token - missing employer or candidate info for order: %s", 
            order.getMerchantTransactionId());
    }
    // ===== END TOKEN GENERATION =====

    LOG.infof("Successfully processed payment for order: %s",
            order.getMerchantTransactionId());
}
```

**Test**: Send success webhook, verify token created with correct employer/candidates.

---

### 🟡 HIGH PRIORITY - Implement Before Production

#### Task 4: Add Fault Tolerance to Odoo Client
**File**: `OdooDocumentClient.java`  
**Effort**: 3 hours  
**Status**: ⚠️ Important

**Problem**: No retry logic, timeouts, or fallbacks for Odoo API calls.

**Implementation**:

1. **Add Dependency** to `pom.xml`:
```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-smallrye-fault-tolerance</artifactId>
</dependency>
```

2. **Add Annotations** to `OdooDocumentClient` methods:
```java
import org.eclipse.microprofile.faulttolerance.*;
import java.time.temporal.ChronoUnit;

@ApplicationScoped
public class OdooDocumentClient {
    
    @Retry(
        maxRetries = 3,
        delay = 1000,
        delayUnit = ChronoUnit.MILLIS,
        jitter = 500,
        retryOn = {Exception.class}
    )
    @Timeout(value = 10, unit = ChronoUnit.SECONDS)
    @Fallback(fallbackMethod = "getCandidateDocumentsFallback")
    public List<OdooDocument> getCandidateDocuments(Integer candidateId) throws Exception {
        ensureAuthenticated();
        // ... existing implementation
    }
    
    // Fallback method
    public List<OdooDocument> getCandidateDocumentsFallback(Integer candidateId) {
        LOG.error("Odoo API unavailable, returning empty document list for candidate: " + candidateId);
        return Collections.emptyList();
    }
    
    @Retry(maxRetries = 3, delay = 1000, jitter = 500)
    @Timeout(value = 10, unit = ChronoUnit.SECONDS)
    public CandidateInfo getCandidateInfo(Integer candidateId) throws Exception {
        // ... existing implementation
    }
    
    @Retry(maxRetries = 3, delay = 1000, jitter = 500)
    @Timeout(value = 15, unit = ChronoUnit.SECONDS)
    public byte[] downloadDocument(final Integer attachmentId) throws Exception {
        // ... existing implementation
    }
}
```

3. **Add Circuit Breaker** (optional but recommended):
```java
@CircuitBreaker(
    requestVolumeThreshold = 10,
    failureRatio = 0.5,
    delay = 5000,
    successThreshold = 3
)
@Retry(maxRetries = 3)
public List<OdooDocument> getCandidateDocuments(Integer candidateId) throws Exception {
    // ... implementation
}
```

**Test**: Mock Odoo to fail, verify retries occur and fallback is invoked.

---

#### Task 5: Add Integration Test for E2E Flow
**File**: Create `PaymentToDocumentFlowIT.java`  
**Effort**: 4 hours  
**Status**: ⚠️ Important

**Implementation**:

```java
@QuarkusTest
@TestProfile(E2ETestProfile.class)
public class PaymentToDocumentFlowIT {
    
    @Inject
    DocumentAccessTokenService tokenService;
    
    @InjectMock
    OdooDocumentClient odooClient;
    
    @Test
    @TestTransaction
    public void testCompleteFlow_PaymentToDocumentDownload() throws Exception {
        // 1. Setup: Mock Odoo to return documents
        when(odooClient.getCandidateDocuments(123))
            .thenReturn(List.of(
                new OdooDocument(456, "Resume.pdf", "base64data...", "application/pdf", 1024)
            ));
        
        // 2. Send payment webhook
        AppyPayWebhookPayload payload = createSuccessPayload();
        
        Response webhookResponse = given()
            .contentType(ContentType.JSON)
            .body(payload)
        .when()
            .post("/webhooks/appypay")
        .then()
            .statusCode(200)
            .extract().response();
        
        // 3. Wait for async processing
        await().atMost(10, SECONDS).untilAsserted(() -> {
            // Verify order is PAID
            // Verify token was generated
        });
        
        // 4. Verify token was created
        List<DocumentAccessTokenEntity> tokens = em.createQuery(
            "SELECT t FROM DocumentAccessToken t WHERE t.employerId = :employerId",
            DocumentAccessTokenEntity.class)
            .setParameter("employerId", "EMP-001")
            .getResultList();
        
        assertEquals(1, tokens.size());
        DocumentAccessTokenEntity token = tokens.get(0);
        assertNotNull(token.getToken());
        assertTrue(token.getExpiresAt().isAfter(Instant.now()));
        
        // 5. Download document using token
        Response downloadResponse = given()
            .queryParam("token", token.getToken())
        .when()
            .get("/api/documents/download/123/456")
        .then()
            .statusCode(200)
            .contentType("application/pdf")
            .header("Content-Disposition", containsString("Resume.pdf"))
            .extract().response();
        
        byte[] documentData = downloadResponse.asByteArray();
        assertTrue(documentData.length > 0);
        
        // 6. Verify audit record created
        List<DocumentAccessAudit> audits = em.createQuery(
            "SELECT a FROM DocumentAccessAudit a WHERE a.documentId = :docId",
            DocumentAccessAudit.class)
            .setParameter("docId", 456)
            .getResultList();
        
        assertEquals(1, audits.size());
    }
    
    private AppyPayWebhookPayload createSuccessPayload() {
        // Create payload with:
        // - status: "Success"
        // - merchantTransactionId linked to order with employerId and candidateIds
        return new AppyPayWebhookPayload(); // TODO: Use builder
    }
}
```

**Test Dependencies**:
```xml
<dependency>
    <groupId>org.awaitility</groupId>
    <artifactId>awaitility</artifactId>
    <version>4.2.0</version>
    <scope>test</scope>
</dependency>
```

---

#### Task 6: Add WireMock for Odoo API Testing
**File**: Create `OdooWireMockSetup.java`  
**Effort**: 2 hours  
**Status**: ⚠️ Important

**Implementation**:

```java
public class OdooWireMockSetup {
    
    private static WireMockServer wireMock;
    
    public static void setupOdooMock() {
        wireMock = new WireMockServer(8069);
        wireMock.start();
        
        // Mock authentication endpoint
        wireMock.stubFor(post(urlPathEqualTo("/xmlrpc/2/common"))
            .willReturn(ok()
                .withHeader("Content-Type", "text/xml")
                .withBody(buildAuthResponse(12345))
            ));
        
        // Mock search_read for documents
        wireMock.stubFor(post(urlPathEqualTo("/xmlrpc/2/object"))
            .withRequestBody(containing("ir.attachment"))
            .withRequestBody(containing("search_read"))
            .willReturn(ok()
                .withHeader("Content-Type", "text/xml")
                .withBody(buildDocumentResponse())
            ));
        
        // Mock read for candidate info
        wireMock.stubFor(post(urlPathEqualTo("/xmlrpc/2/object"))
            .withRequestBody(containing("hr.applicant"))
            .withRequestBody(containing("\"read\""))
            .willReturn(ok()
                .withHeader("Content-Type", "text/xml")
                .withBody(buildCandidateResponse())
            ));
    }
    
    private static String buildAuthResponse(int uid) {
        return String.format("""
            <?xml version='1.0'?>
            <methodResponse>
                <params>
                    <param>
                        <value><int>%d</int></value>
                    </param>
                </params>
            </methodResponse>
            """, uid);
    }
    
    private static String buildDocumentResponse() {
        return """
            <?xml version='1.0'?>
            <methodResponse>
                <params>
                    <param>
                        <value>
                            <array>
                                <data>
                                    <value>
                                        <struct>
                                            <member>
                                                <name>id</name>
                                                <value><int>456</int></value>
                                            </member>
                                            <member>
                                                <name>name</name>
                                                <value><string>Resume.pdf</string></value>
                                            </member>
                                            <member>
                                                <name>datas</name>
                                                <value><string>JVBERi0xLjQKJeLjz9M...</string></value>
                                            </member>
                                            <member>
                                                <name>mimetype</name>
                                                <value><string>application/pdf</string></value>
                                            </member>
                                            <member>
                                                <name>file_size</name>
                                                <value><int>102400</int></value>
                                            </member>
                                        </struct>
                                    </value>
                                </data>
                            </array>
                        </value>
                    </param>
                </params>
            </methodResponse>
            """;
    }
    
    public static void tearDown() {
        if (wireMock != null) {
            wireMock.stop();
        }
    }
}
```

**Usage in Tests**:
```java
@QuarkusTest
public class OdooDocumentClientIntegrationTest {
    
    @BeforeAll
    static void setup() {
        OdooWireMockSetup.setupOdooMock();
    }
    
    @AfterAll
    static void tearDown() {
        OdooWireMockSetup.tearDown();
    }
    
    @Test
    public void testGetCandidateDocuments_Success() throws Exception {
        // Test will use mocked Odoo responses
    }
}
```

---

### 🟢 MEDIUM PRIORITY - Improvements

#### Task 7: Add Unit Tests for Missing Coverage

**Create these test files**:

1. **`OdooDocumentClientTest.java`** (currently missing):
```java
@QuarkusTest
public class OdooDocumentClientTest {
    
    @InjectMock
    XmlRpcClient mockModelsClient;
    
    @Inject
    OdooDocumentClient client;
    
    @Test
    public void testGetCandidateInfo_Success() throws Exception {
        // Mock XML-RPC response
        Object[] mockResponse = new Object[]{
            Map.of(
                "id", 123,
                "partner_name", "John Doe",
                "email_from", "john@example.com"
            )
        };
        when(mockModelsClient.execute(eq("execute_kw"), any()))
            .thenReturn(mockResponse);
        
        // Execute
        CandidateInfo info = client.getCandidateInfo(123);
        
        // Assert
        assertNotNull(info);
        assertEquals(123, info.id());
        assertEquals("John Doe", info.name());
        assertEquals("john@example.com", info.email());
    }
    
    @Test
    public void testGetCandidateInfo_NotFound() {
        // Mock empty response
        when(mockModelsClient.execute(any(), any())).thenReturn(new Object[]{});
        
        // Assert throws
        assertThrows(IllegalArgumentException.class, () -> {
            client.getCandidateInfo(999999);
        });
    }
    
    // Add more tests...
}
```

2. **`WebhookProcessorTest.java`** (currently missing):
```java
@QuarkusTest
public class WebhookProcessorTest {
    
    @InjectMock
    PaymentProcessService paymentService;
    
    @InjectMock
    WebhookEventServiceFacade webhookEventService;
    
    @Inject
    WebhookProcessor processor;
    
    @Test
    public void testProcessPayment_Success() {
        AppyPayWebhookPayload payload = createTestPayload();
        
        // Execute
        processor.processPayment(payload);
        
        // Verify
        verify(webhookEventService).markAsProcessing(payload.getId());
        verify(paymentService).processPaymentStatus(payload);
        verify(webhookEventService).markAsProcessed(payload.getId());
    }
    
    @Test
    public void testProcessPayment_FailureHandling() {
        AppyPayWebhookPayload payload = createTestPayload();
        
        // Mock failure
        doThrow(new RuntimeException("DB error"))
            .when(paymentService).processPaymentStatus(any());
        
        // Execute and assert
        assertThrows(RuntimeException.class, () -> {
            processor.processPayment(payload);
        });
        
        // Verify failure marked
        verify(webhookEventService).markAsFailed(eq(payload.getId()), anyString());
    }
}
```

---

#### Task 8: Add TestContainers Support

**File**: Create `DatabaseTestResource.java`

```java
public class DatabaseTestResource implements QuarkusTestResourceLifecycleManager {
    
    private static final PostgreSQLContainer<?> POSTGRES = 
        new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);
    
    @Override
    public Map<String, String> start() {
        POSTGRES.start();
        
        return Map.of(
            "quarkus.datasource.jdbc.url", POSTGRES.getJdbcUrl(),
            "quarkus.datasource.username", POSTGRES.getUsername(),
            "quarkus.datasource.password", POSTGRES.getPassword()
        );
    }
    
    @Override
    public void stop() {
        // Container will be reused
    }
}
```

**Usage**:
```java
@QuarkusTest
@QuarkusTestResource(DatabaseTestResource.class)
public class MyIntegrationTest {
    // Will use real PostgreSQL via TestContainers
}
```

**Add to `pom.xml`**:
```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <version>1.19.8</version>
    <scope>test</scope>
</dependency>
```

---

## Quick Command Reference

### Run Tests
```bash
# All tests
./mvnw test

# Unit tests only
./mvnw test -Dtest=*Test

# Integration tests only
./mvnw verify -Dtest=*IT

# Specific test class
./mvnw test -Dtest=OdooDocumentClientTest

# With coverage report
./mvnw test jacoco:report
# Report: target/site/jacoco/index.html
```

### Database Commands
```bash
# Apply migrations
./mvnw flyway:migrate

# Rollback migration
./mvnw flyway:undo

# Check migration status
./mvnw flyway:info
```

### Run Application
```bash
# Dev mode with live reload
./mvnw quarkus:dev

# Production build
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar
```

### Environment Setup
```bash
# Required environment variables
export ODOO_URL=http://odoo-server:8069
export ODOO_DATABASE=production_db
export ODOO_USERNAME=api_user
export ODOO_PASSWORD=secure_password
export JWT_SECRET=your-256-bit-secret-key
export DB_URL=jdbc:postgresql://localhost:5432/oportunidade
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
```

---

## Estimated Timeline

| Task | Priority | Effort | Dependencies | Assigned To |
|------|----------|--------|--------------|-------------|
| Task 1: Fix Odoo Config | 🔴 Critical | 1 hour | None | Backend Dev |
| Task 2: Payment-Document Mapping | 🔴 Critical | 4 hours | None | Backend Dev |
| Task 3: Payment → Token | 🔴 Critical | 2 hours | Task 1, 2 | Backend Dev |
| Task 4: Fault Tolerance | 🟡 High | 3 hours | Task 1 | Backend Dev |
| Task 5: E2E Test | 🟡 High | 4 hours | Task 1-3 | QA/Backend |
| Task 6: WireMock Setup | 🟡 High | 2 hours | None | Backend Dev |
| Task 7: Unit Tests | 🟢 Medium | 8 hours | Task 1 | QA/Backend |
| Task 8: TestContainers | 🟢 Medium | 2 hours | None | Backend Dev |

**Total Estimated Effort**: ~26 hours (~3-4 working days)

---

## Definition of Done

### For Critical Tasks (1-3):
- [ ] Code implemented and committed
- [ ] Unit tests written and passing
- [ ] Integration test added (if applicable)
- [ ] Code reviewed by at least one other developer
- [ ] Documentation updated
- [ ] Manual testing completed
- [ ] No new linter errors
- [ ] Environment variables documented

### For High Priority Tasks (4-6):
- [ ] Code implemented
- [ ] Tests passing
- [ ] Code reviewed
- [ ] Deployed to staging environment
- [ ] Load tested (for fault tolerance)

### For Medium Priority Tasks (7-8):
- [ ] Code implemented
- [ ] Tests passing
- [ ] Code reviewed

---

## Success Metrics

### Code Quality
- [ ] Code coverage ≥ 75%
- [ ] All critical paths tested
- [ ] No blocker/critical bugs in SonarQube
- [ ] All security vulnerabilities addressed

### Functionality
- [ ] Payment webhook processed in < 5 seconds
- [ ] Token generated automatically on success
- [ ] Document download works end-to-end
- [ ] Access audit logs created correctly

### Performance
- [ ] 100 concurrent webhooks processed without errors
- [ ] Document download < 3 seconds (for 5MB file)
- [ ] Odoo API calls succeed with 99.9% reliability
- [ ] Rate limiting prevents API overload

### Reliability
- [ ] System recovers from Odoo downtime
- [ ] Retry logic works correctly
- [ ] No data loss on failures
- [ ] Circuit breaker prevents cascade failures

---

**Last Updated**: 2026-02-16  
**Maintainer**: Development Team  
**Status**: Ready for Implementation
