# Architecture Diagrams and Gap Visualization

## 1. Current System Architecture

```mermaid
graph TB
    subgraph "External Systems"
        AppyPay[AppyPay/MultiCaixa]
        Odoo[Odoo SaaS]
    end
    
    subgraph "Quarkus Application"
        WebhookResource[AppyPayWebhookResource<br/>POST /webhooks/appypay]
        WebhookProcessor[WebhookProcessor<br/>@Blocking Async]
        PaymentService[PaymentProcessService<br/>handleSuccessfulPayment]
        OdooClient[OdooDocumentClient<br/>XML-RPC]
        TokenService[DocumentAccessTokenService<br/>generateToken]
        DocumentResource[DocumentAccessResource<br/>/api/documents/*]
        
        WebhookEvents[(webhook_events)]
        Orders[(orders)]
        PaymentTx[(payment_transactions)]
        Tokens[(document_access_tokens)]
        Audits[(document_access_audit)]
    end
    
    subgraph "Employer"
        Browser[Employer Browser]
    end
    
    AppyPay -->|1. Payment Webhook| WebhookResource
    WebhookResource -->|2. Queue Event| WebhookProcessor
    WebhookProcessor -->|3. Process Payment| PaymentService
    PaymentService -->|4. Save| Orders
    PaymentService -->|5. Create| PaymentTx
    PaymentService -->|6. Notify| Odoo
    
    PaymentService -.->|❌ MISSING LINK| TokenService
    
    TokenService -->|7. Create Token| Tokens
    
    Browser -->|8. Request Documents| DocumentResource
    DocumentResource -->|9. Validate Token| TokenService
    DocumentResource -->|10. Fetch Documents| OdooClient
    OdooClient -->|11. XML-RPC| Odoo
    Odoo -->|12. Return Documents| OdooClient
    OdooClient -->|13. Return| DocumentResource
    DocumentResource -->|14. Log Access| Audits
    DocumentResource -->|15. Download| Browser
    
    WebhookResource -->|Save| WebhookEvents
    
    style PaymentService fill:#ff9999
    style TokenService fill:#ff9999
    style OdooClient fill:#ffcc99
```

### Legend
- 🔴 **Red**: Components with critical gaps
- 🟠 **Orange**: Components with issues
- ⚫ **Black Lines**: Existing connections
- 🔴 **Red Dashed**: Missing connections

---

## 2. Payment-to-Document Flow (With Gaps Highlighted)

```mermaid
sequenceDiagram
    participant AppyPay
    participant Webhook as WebhookResource
    participant Processor as WebhookProcessor
    participant Payment as PaymentProcessService
    participant Order as OrderService
    participant Token as TokenService
    participant Odoo as OdooClient
    participant OdooAPI as Odoo SaaS
    
    AppyPay->>Webhook: POST /webhooks/appypay<br/>{status: "Success"}
    Webhook->>Webhook: Save to webhook_events
    Webhook-->>AppyPay: 200 OK (immediate)
    
    Webhook->>Processor: Queue for async processing
    
    Processor->>Payment: processPaymentStatus(payload)
    
    Payment->>Order: find(merchantTransactionId)
    Order-->>Payment: Order entity
    
    Payment->>Order: setStatus(PAID)
    Payment->>Payment: createPaymentTransaction()
    
    Payment->>OdooAPI: sendPaymentToOdoo()
    OdooAPI-->>Payment: Success
    
    rect rgb(255, 200, 200)
        Note over Payment,Token: ❌ MISSING: Auto token generation
        Payment->>Token: generateToken(employerId, candidateIds, packageType)
        Note over Payment,Token: Currently NOT implemented
    end
    
    rect rgb(255, 220, 200)
        Note over Order: ⚠️ ISSUE: Missing fields
        Note over Order: - employerId<br/>- candidateIds<br/>- packageType
    end
    
    Token->>Token: Create JWT
    Token->>Token: Save to document_access_tokens
    
    Note over Token: Should send token to employer<br/>(via email/webhook)
```

---

## 3. Document Access Flow (Current vs Expected)

### Current Implementation
```mermaid
graph LR
    subgraph "Current Flow ❌"
        E1[Employer] -->|Manual Token Request?| T1[TokenService]
        T1 -->|Generate| TOK1[Token]
        E1 -->|Download with Token| D1[DocumentResource]
        D1 -->|Validate| T1
        D1 -->|Fetch| O1[OdooClient]
        O1 -.->|⚠️ Wrong Config| ODOO1[(Odoo)]
        
        style O1 fill:#ffcc99
        style ODOO1 fill:#ff9999
    end
```

### Expected Implementation
```mermaid
graph LR
    subgraph "Expected Flow ✅"
        PAY[Payment Success] -->|Auto Generate| T2[TokenService]
        T2 -->|Send via Email| E2[Employer]
        E2 -->|Download with Token| D2[DocumentResource]
        D2 -->|Validate| T2
        D2 -->|Fetch| O2[OdooClient]
        O2 -->|Correct Config| ODOO2[(Odoo)]
        O2 -->|With Retry| O2
        
        style PAY fill:#99ff99
        style T2 fill:#99ff99
        style O2 fill:#99ff99
    end
```

---

## 4. Data Model with Mapping Gaps

```mermaid
erDiagram
    ORDERS ||--o{ PAYMENT_TRANSACTIONS : "has"
    ORDERS ||--o{ ORDER_CANDIDATE_MAPPING : "maps to"
    ORDER_CANDIDATE_MAPPING }o--|| ODOO_CANDIDATES : "references"
    ODOO_CANDIDATES ||--o{ ODOO_DOCUMENTS : "has"
    DOCUMENT_ACCESS_TOKENS ||--o{ TOKEN_CANDIDATE_IDS : "includes"
    TOKEN_CANDIDATE_IDS }o--|| ODOO_CANDIDATES : "references"
    DOCUMENT_ACCESS_AUDIT }o--|| ODOO_DOCUMENTS : "logs"
    
    ORDERS {
        uuid id PK
        string merchantTransactionId UK
        uuid reference_id FK "⚠️ Not to Odoo!"
        decimal amount
        string status
        string employer_id "❌ MISSING"
        int job_id "❌ MISSING"
        string package_type "❌ MISSING"
    }
    
    ORDER_CANDIDATE_MAPPING {
        uuid order_id FK "❌ TABLE MISSING"
        int candidate_id FK "❌ TABLE MISSING"
    }
    
    PAYMENT_TRANSACTIONS {
        uuid id PK
        uuid order_id FK
        string appypayTransactionId
        decimal amount
        string status
    }
    
    DOCUMENT_ACCESS_TOKENS {
        uuid id PK
        string token UK
        string employer_id
        int max_downloads
        timestamp expires_at
    }
    
    TOKEN_CANDIDATE_IDS {
        uuid token_id FK
        int candidate_id
    }
    
    ODOO_CANDIDATES {
        int id PK "External: hr.applicant"
        string name
        string email
        int job_id
    }
    
    ODOO_DOCUMENTS {
        int id PK "External: ir.attachment"
        string name
        string mimetype
        string datas "Base64"
    }
    
    DOCUMENT_ACCESS_AUDIT {
        uuid id PK
        string token
        int candidate_id
        int document_id
        timestamp accessed_at
        string ip_address
    }
```

### Legend
- ✅ **Exists**: Implemented correctly
- ⚠️ **Issue**: Exists but has problems
- ❌ **Missing**: Not implemented

---

## 5. Configuration Issues

### Current Configuration (WRONG ❌)
```yaml
# application.yml
quarkus:
  datasource:
    db-kind: postgresql          # ← OdooClient reads this as Odoo DB name! ❌
    username: postgres           # ← Database user, not Odoo user! ❌
    password: postgres           # ← Database password, not Odoo password! ❌
    jdbc:
      url: jdbc:postgresql://localhost:5432/odoo_payments
```

```java
// OdooDocumentClient.java (WRONG)
@ConfigProperty(name = "quarkus.datasource.db-kind")
String database;  // Will be "postgresql", not Odoo database name!

@ConfigProperty(name = "quarkus.datasource.username")
String username;  // Will be "postgres", not Odoo username!
```

### Expected Configuration (CORRECT ✅)
```yaml
# application.yml
odoo:
  url: ${ODOO_URL:http://localhost:8069}
  database: ${ODOO_DATABASE:odoo}      # ← Correct Odoo database
  username: ${ODOO_USERNAME:admin}     # ← Correct Odoo user
  password: ${ODOO_PASSWORD}           # ← Correct Odoo password

jwt:
  secret: ${JWT_SECRET}                # ← Currently missing!
  issuer: document-service
```

```java
// OdooDocumentClient.java (CORRECT)
@ConfigProperty(name = "odoo.url")
String odooUrl;

@ConfigProperty(name = "odoo.database")
String database;

@ConfigProperty(name = "odoo.username")
String username;

@ConfigProperty(name = "odoo.password")
String password;
```

---

## 6. Test Coverage Visualization

```mermaid
pie title "Current Test Coverage by Component"
    "OdooDocumentClient" : 0
    "DocumentAccessTokenService" : 60
    "PaymentProcessService" : 50
    "DocumentAccessResource" : 40
    "WebhookProcessor" : 0
    "Integration Tests" : 20
    "E2E Tests" : 0
```

### Target Test Coverage
```mermaid
pie title "Target Test Coverage"
    "OdooDocumentClient" : 85
    "DocumentAccessTokenService" : 90
    "PaymentProcessService" : 80
    "DocumentAccessResource" : 75
    "WebhookProcessor" : 70
    "Integration Tests" : 60
    "E2E Tests" : 30
```

---

## 7. Error Handling Flow (Current vs Expected)

### Current: No Error Recovery ❌
```mermaid
graph TB
    A[OdooClient.getCandidateDocuments] -->|Call Odoo| B{Odoo Response}
    B -->|Success| C[Return Documents]
    B -->|Failure| D[Throw Exception]
    D --> E[Application Error]
    E --> F[User Sees Error]
    
    style D fill:#ff9999
    style E fill:#ff9999
    style F fill:#ff9999
```

### Expected: With Fault Tolerance ✅
```mermaid
graph TB
    A[OdooClient.getCandidateDocuments<br/>@Retry @Timeout @Fallback] -->|Call 1| B{Odoo Response}
    B -->|Failure| C[Wait 1s]
    C -->|Call 2| D{Odoo Response}
    D -->|Failure| E[Wait 1.5s]
    E -->|Call 3| F{Odoo Response}
    F -->|Failure| G[Invoke Fallback]
    G --> H[Return Empty List + Log Error]
    H --> I[User Sees Graceful Message]
    
    B -->|Success| J[Return Documents]
    D -->|Success| J
    F -->|Success| J
    
    A -->|Timeout > 10s| K[Cancel Request]
    K --> G
    
    style G fill:#99ff99
    style H fill:#99ff99
    style I fill:#99ff99
    style J fill:#99ff99
```

---

## 8. Integration Points Summary

| Integration | Protocol | Status | Issues |
|-------------|----------|--------|--------|
| **AppyPay → Backend** | REST Webhook | ✅ Working | None |
| **Backend → Odoo (Payment)** | REST | ✅ Working | Uses `OdooApiClient` correctly |
| **Backend → Odoo (Documents)** | XML-RPC | ❌ Broken | Wrong credentials configuration |
| **Employer → Backend (Tokens)** | N/A | ❌ Missing | No auto-generation |
| **Employer → Backend (Download)** | REST | ⚠️ Partial | Works if token exists, but tokens not created |

---

## 9. Critical Path Analysis

### Happy Path (Should Work)
```mermaid
graph LR
    A[Payment Success] -->|1| B[Webhook Received]
    B -->|2| C[Order Created]
    C -->|3| D[Transaction Saved]
    D -->|4| E[Odoo Notified]
    E -->|❌ 5. MISSING| F[Token Generated]
    F -->|6| G[Employer Notified]
    G -->|7| H[Employer Downloads]
    
    style A fill:#99ff99
    style B fill:#99ff99
    style C fill:#99ff99
    style D fill:#99ff99
    style E fill:#99ff99
    style F fill:#ff9999
    style G fill:#ff9999
    style H fill:#ff9999
```

### Current Reality (Broken)
```mermaid
graph LR
    A[Payment Success] -->|✅| B[Webhook Received]
    B -->|✅| C[Order Created]
    C -->|✅| D[Transaction Saved]
    D -->|⚠️| E[Odoo Notified<br/>May Fail]
    E -.->|❌| X[STOPS HERE]
    X -.->|Manual?| F[Token Generated]
    F -->|✅| G[Employer Downloads]
    G -.->|❌| H[Odoo Fetch Fails<br/>Wrong Config]
    
    style A fill:#99ff99
    style B fill:#99ff99
    style C fill:#99ff99
    style D fill:#99ff99
    style E fill:#ffcc99
    style X fill:#ff0000
    style F fill:#ff9999
    style H fill:#ff0000
```

---

## 10. Implementation Priority Heat Map

```mermaid
graph TB
    subgraph "Critical Path - Do First 🔴"
        G1[Fix Odoo Config<br/>1 hour]
        G2[Add Order Mapping<br/>4 hours]
        G3[Payment → Token<br/>2 hours]
    end
    
    subgraph "High Priority - Do Next 🟡"
        G4[Fault Tolerance<br/>3 hours]
        G5[E2E Test<br/>4 hours]
        G6[WireMock Setup<br/>2 hours]
    end
    
    subgraph "Medium Priority - Can Wait 🟢"
        G7[Unit Tests<br/>8 hours]
        G8[TestContainers<br/>2 hours]
        G9[Rate Limiting<br/>3 hours]
    end
    
    G1 --> G3
    G2 --> G3
    G3 --> G4
    G3 --> G5
    G1 --> G6
    G6 --> G7
    
    style G1 fill:#ff6666
    style G2 fill:#ff6666
    style G3 fill:#ff6666
    style G4 fill:#ffcc66
    style G5 fill:#ffcc66
    style G6 fill:#ffcc66
    style G7 fill:#99ff99
    style G8 fill:#99ff99
    style G9 fill:#99ff99
```

---

## 11. Risk Heat Map

```mermaid
quadrantChart
    title Risk Assessment
    x-axis Low Impact --> High Impact
    y-axis Low Probability --> High Probability
    quadrant-1 Critical Risk
    quadrant-2 Monitor
    quadrant-3 Low Priority
    quadrant-4 Mitigate
    
    Odoo Config Wrong: [0.95, 0.95]
    Token Not Generated: [0.90, 0.85]
    No Payment Mapping: [0.85, 0.80]
    No Retry Logic: [0.70, 0.60]
    Missing JWT Secret: [0.75, 0.50]
    No Rate Limiting: [0.65, 0.40]
    Low Test Coverage: [0.55, 0.30]
```

### Risk Legend
- **Quadrant 1 (Top Right)**: Critical - Fix immediately
- **Quadrant 2 (Top Left)**: Monitor closely
- **Quadrant 3 (Bottom Left)**: Low priority
- **Quadrant 4 (Bottom Right)**: Mitigate soon

---

## 12. System State Diagram

```mermaid
stateDiagram-v2
    [*] --> WebhookReceived: Payment Notification
    WebhookReceived --> OrderCreated: Valid Payload
    OrderCreated --> PaymentProcessed: Status = Success
    PaymentProcessed --> OdooNotified: Send to Odoo
    
    OdooNotified --> TokenGenerated: ❌ MISSING TRANSITION
    OdooNotified --> OrderComplete: ⚠️ CURRENTLY STOPS
    
    TokenGenerated --> EmployerNotified: Send Email/Webhook
    EmployerNotified --> AwaitingDownload: Token Ready
    
    AwaitingDownload --> DocumentRequested: Employer Accesses
    DocumentRequested --> TokenValidated: Check Token
    
    TokenValidated --> OdooFetched: ⚠️ Credentials Issue
    TokenValidated --> AccessDenied: Invalid Token
    
    OdooFetched --> DocumentServed: Success
    OdooFetched --> FetchFailed: ❌ No Retry Logic
    
    DocumentServed --> AuditLogged: Record Access
    AuditLogged --> [*]: Complete
    
    AccessDenied --> [*]: Reject
    FetchFailed --> [*]: Error
    OrderComplete --> [*]: ⚠️ Incomplete Flow
```

---

## Summary of Architectural Gaps

### 🔴 **Critical Gaps**
1. ❌ Odoo authentication uses wrong credentials
2. ❌ No payment-to-token auto-generation
3. ❌ Missing payment-document mapping

### 🟡 **Important Issues**
4. ⚠️ No error recovery for Odoo failures
5. ⚠️ Missing JWT secret configuration
6. ⚠️ No integration/E2E tests

### 🟢 **Nice to Have**
7. ⚠️ No rate limiting
8. ⚠️ Low unit test coverage
9. ⚠️ No monitoring/alerting

---

**Document Version**: 1.0  
**Last Updated**: 2026-02-16  
**Next Review**: After implementing critical tasks
