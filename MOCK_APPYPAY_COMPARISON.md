# Mock AppyPay: Approach Comparison

## Quick Answer

**For automated testing**: ✅ **Use WireMock** (already in your dependencies)  
**For manual testing/demos**: Consider standalone app with UI  
**For unit tests**: ✅ **Use Quarkus @InjectMock** (already using)

## Approach 1: WireMock (RECOMMENDED for Testing)

### ✅ Advantages
- **Already in your pom.xml** - No new dependencies
- **Perfect for integration tests** - Designed for this
- **Stateful scenarios** - Can simulate complex flows
- **Request matching** - Match by headers, body, path
- **Response templating** - Dynamic responses
- **Fault injection** - Test retries, timeouts, errors
- **No separate app** - Runs in-process with tests
- **Programmatic control** - Full API in Java

### 📝 Example Usage

```java
// In your integration test
@QuarkusTest
public class AppyPayWebhookIntegrationTest {
    
    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().port(3000))
        .build();
    
    @Test
    void testSuccessfulPaymentFlow() {
        // Given - Mock AppyPay behavior
        UUID txId = UUID.randomUUID();
        wireMock.stubFor(post("/appypay/create-reference")
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "referenceNumber": "123456789",
                        "entity": "00123",
                        "status": "ACTIVE"
                    }
                    """)));
        
        // When - Simulate AppyPay sending webhook to OUR backend
        AppyPayWebhookPayload payload = AppyPayWebhookPayload.builder()
            .id(txId.toString())
            .merchantTransactionId("ORDER-123")
            .amount(new BigDecimal("1500.00"))
            .currency("AOA")
            .status("Success")
            .paymentMethod("REF")
            .build();
        
        given()
            .contentType(ContentType.JSON)
            .body(payload)
        .when()
            .post("/webhooks/appypay")
        .then()
            .statusCode(200);
        
        // Then - Verify our backend processed it
        await().atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                Order order = Order.find("merchantTransactionId", "ORDER-123").firstResult();
                assertNotNull(order);
                assertEquals(OrderStatus.COMPLETED, order.getStatus());
            });
    }
    
    @Test
    void testRetryOnFailure() {
        // Simulate webhook sending failure then success
        wireMock.stubFor(post("/external/odoo-api")
            .inScenario("Retry Scenario")
            .whenScenarioStateIs(STARTED)
            .willReturn(aResponse().withStatus(500))
            .willSetStateTo("First Attempt Failed"));
        
        wireMock.stubFor(post("/external/odoo-api")
            .inScenario("Retry Scenario")
            .whenScenarioStateIs("First Attempt Failed")
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("{\"status\": \"success\"}")));
        
        // Test your retry logic...
    }
    
    @Test
    void testWebhookWithDelay() {
        // Simulate slow AppyPay response
        wireMock.stubFor(post("/appypay/webhook")
            .willReturn(aResponse()
                .withStatus(200)
                .withFixedDelay(3000))); // 3 second delay
    }
}
```

### 🔧 Standalone WireMock Server (for manual testing)

You can also run WireMock as a standalone server:

```bash
# Download WireMock
wget https://repo1.maven.org/maven2/org/wiremock/wiremock-standalone/3.3.1/wiremock-standalone-3.3.1.jar

# Run standalone
java -jar wiremock-standalone-3.3.1.jar --port 3000

# Configure mappings in mappings/ folder
```

**mappings/success-payment.json:**
```json
{
  "request": {
    "method": "GET",
    "url": "/trigger-success-webhook"
  },
  "response": {
    "status": 200,
    "headers": {
      "Content-Type": "application/json"
    },
    "postServeActions": {
      "webhook": {
        "url": "http://localhost:8080/webhooks/appypay",
        "method": "POST",
        "headers": {
          "Content-Type": "application/json"
        },
        "body": "{{jsonPath request.body '$.'}}"
      }
    }
  }
}
```

### 📦 Add to pom.xml (if not already present)

```xml
<dependency>
    <groupId>org.wiremock</groupId>
    <artifactId>wiremock</artifactId>
    <version>3.3.1</version>
    <scope>test</scope>
</dependency>
```

---

## Approach 2: Quarkus @InjectMock (for Unit Tests)

### ✅ Advantages
- **Already using it** - No new tools
- **Fast** - Pure unit tests
- **Mockito integration** - Familiar API
- **Type-safe** - Compile-time checking

### 📝 Example Usage

```java
@QuarkusTest
public class PaymentProcessServiceTest {
    
    @InjectMock
    OdooPaymentService odooPaymentService;
    
    @InjectMock
    DocumentTokenService tokenService;
    
    @Inject
    PaymentProcessService paymentProcessService;
    
    @Test
    void testProcessSuccessfulPayment() {
        // Given - Mock dependencies
        AppyPayWebhookPayload payload = createTestPayload();
        
        when(tokenService.generateAccessToken(any()))
            .thenReturn("mock-jwt-token");
        
        when(tokenService.generateDownloadUrl(any()))
            .thenReturn("https://example.com/download?token=...");
        
        doNothing().when(odooPaymentService)
            .sendPaymentToOdoo(any());
        
        // When
        paymentProcessService.processPaymentStatus(payload);
        
        // Then
        verify(tokenService, times(1)).generateAccessToken(any());
        verify(odooPaymentService, times(1)).sendPaymentToOdoo(any());
    }
}
```

### ❌ Limitation
- **Unit tests only** - Doesn't test actual HTTP integration
- **No real webhook simulation** - Can't test the full flow

---

## Approach 3: Quarkus Test Resources (for Integration Tests)

### 📝 Example Usage

```java
// Custom test resource that starts a mock AppyPay
public class MockAppyPayResource implements QuarkusTestResourceLifecycleManager {
    
    private Server mockServer;
    
    @Override
    public Map<String, String> start() {
        // Start embedded Jetty server
        mockServer = new Server(3000);
        ServletContextHandler handler = new ServletContextHandler();
        handler.addServlet(new ServletHolder(new MockAppyPayServlet()), "/*");
        mockServer.setHandler(handler);
        
        try {
            mockServer.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        return Map.of(
            "appypay.mock.url", "http://localhost:3000",
            "appypay.mock.enabled", "true"
        );
    }
    
    @Override
    public void stop() {
        if (mockServer != null) {
            try {
                mockServer.stop();
            } catch (Exception e) {
                // ignore
            }
        }
    }
}

// Use in tests
@QuarkusTest
@QuarkusTestResource(MockAppyPayResource.class)
public class AppyPayIntegrationTest {
    // Tests here will have mock AppyPay running
}
```

---

## Approach 4: Standalone Mock App with UI

### ✅ Advantages
- **Visual interface** - Non-developers can use
- **Manual testing** - Good for demos
- **Independent** - Doesn't affect test runtime
- **Realistic** - Feels like real AppyPay portal

### ❌ Disadvantages
- **Extra maintenance** - Separate codebase
- **Not automated** - Manual steps required
- **Different tech stack** - Node.js/Python/etc
- **Setup overhead** - Need to run 2 applications

### 🎯 When to Use
- Client demos
- Manual QA testing
- Training/documentation
- Exploratory testing

---

## Recommendation by Use Case

| Use Case | Recommended Approach | Complexity | Setup Time |
|----------|---------------------|------------|------------|
| **Unit Tests** | Quarkus @InjectMock | ⭐ Easy | Already done |
| **Integration Tests** | WireMock | ⭐⭐ Medium | 30 minutes |
| **Automated E2E Tests** | WireMock + TestContainers | ⭐⭐⭐ Complex | 2 hours |
| **Manual Testing** | Standalone App | ⭐⭐⭐⭐ High | 4-8 hours |
| **Demo/Training** | Standalone App | ⭐⭐⭐⭐ High | 4-8 hours |
| **CI/CD Pipeline** | WireMock | ⭐⭐ Medium | 1 hour |

---

## Quick Implementation: WireMock for Your Tests

Since you already have WireMock in dependencies, here's a quick setup:

### 1. Create WireMock Test Base Class

```java
// src/test/java/ao/co/oportunidade/test/WireMockTestBase.java
package ao.co.oportunidade.test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@QuarkusTest
public abstract class WireMockTestBase {
    
    protected static WireMockServer wireMockServer;
    
    @BeforeAll
    static void setupWireMock() {
        wireMockServer = new WireMockServer(
            WireMockConfiguration.options()
                .port(9999) // Different from your app
                .dynamicPort()
        );
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());
    }
    
    @AfterAll
    static void teardownWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }
    
    // Helper method to create success webhook
    protected void mockSuccessfulPayment(String txId, String merchantTxId) {
        stubFor(post(urlEqualTo("/webhooks/appypay"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(String.format("""
                    {
                        "status": "received",
                        "message": "Webhook received",
                        "eventId": "%s"
                    }
                    """, txId))));
    }
    
    // Helper for failed payment
    protected void mockFailedPayment(String txId) {
        stubFor(post(urlEqualTo("/webhooks/appypay"))
            .willReturn(aResponse()
                .withStatus(500)
                .withBody("Internal Server Error")));
    }
    
    // Helper for delayed response
    protected void mockDelayedPayment(String txId, int delayMs) {
        stubFor(post(urlEqualTo("/webhooks/appypay"))
            .willReturn(aResponse()
                .withStatus(200)
                .withFixedDelay(delayMs)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"status\": \"received\"}")));
    }
}
```

### 2. Use in Your Tests

```java
public class PaymentFlowWithWireMockTest extends WireMockTestBase {
    
    @Test
    void testCompletePaymentFlow() {
        // Given - Mock external Odoo API
        mockSuccessfulOdooResponse();
        
        // When - Send webhook to our backend
        AppyPayWebhookPayload payload = createPayload();
        given()
            .contentType(ContentType.JSON)
            .body(payload)
        .when()
            .post("/webhooks/appypay")
        .then()
            .statusCode(200);
        
        // Then - Verify WireMock received expected calls
        verify(postRequestedFor(urlEqualTo("/odoo/api/payment"))
            .withHeader("Content-Type", equalTo("application/json")));
    }
    
    @Test
    void testRetryOnOdooFailure() {
        // First call fails, second succeeds
        stubFor(post("/odoo/api/payment")
            .inScenario("Retry")
            .whenScenarioStateIs(STARTED)
            .willReturn(aResponse().withStatus(500))
            .willSetStateTo("First Failed"));
        
        stubFor(post("/odoo/api/payment")
            .inScenario("Retry")
            .whenScenarioStateIs("First Failed")
            .willReturn(aResponse().withStatus(200)));
        
        // Test your retry logic...
    }
}
```

---

## Final Recommendation

### For You (Based on Current Project):

1. **Now (High Priority)**: 
   - ✅ Use **WireMock** for your failing integration tests
   - Already in dependencies
   - Fix the 47 failing tests quickly
   - Achieve 75%+ coverage

2. **Soon (Medium Priority)**:
   - Keep using **@InjectMock** for unit tests (already working well)
   - Add more WireMock scenarios for edge cases

3. **Later (Low Priority)**:
   - **Standalone App** only if you need:
     - Client demos
     - Manual QA team testing
     - Non-technical users to trigger webhooks

### Cost-Benefit Analysis

| Approach | Setup Time | Test Value | Maintenance | Automation |
|----------|-----------|------------|-------------|------------|
| WireMock | 1-2 hours | ⭐⭐⭐⭐⭐ | Low | ✅ Full |
| Standalone App | 8+ hours | ⭐⭐⭐ | High | ❌ Manual |

**Verdict**: Start with WireMock. Only build standalone app if you have specific manual testing needs.

---

## Next Steps

Want me to:
1. ✅ Create WireMock test examples for your failing integration tests?
2. ✅ Fix the 47 failing tests using WireMock mocks?
3. ✅ Set up WireMock base class for your project?
4. ❌ Still build the standalone app (if you need it for demos)?

Let me know!
