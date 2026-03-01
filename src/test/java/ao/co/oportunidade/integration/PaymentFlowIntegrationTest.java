package ao.co.oportunidade.integration;

import ao.co.oportunidade.employer.model.EmployerReference;
import ao.co.oportunidade.notification.service.AlertService;
import ao.co.oportunidade.order.entity.OrderEntity;
import ao.co.oportunidade.order.model.PackageType;
import ao.co.oportunidade.payment.service.NotificationService;
import ao.co.oportunidade.payment.service.PaymentProcessService;
import ao.co.oportunidade.webhook.dto.AppyPayWebhookPayload;
import ao.co.oportunidade.webhook.dto.CustomerInfo;
import ao.co.oportunidade.webhook.dto.ReferenceInfo;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import solutions.envision.odoo.document.service.DocumentTokenService;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration tests for the complete payment processing flow.
 *
 * Architecture:
 *   AppyPay → POST /webhooks/appypay → PaymentProcessService
 *                                              ├── OdooPaymentService  → WireMock (port 9090)
 *                                              ├── DocumentTokenService → @InjectMock (local JWT)
 *                                              ├── NotificationService  → @InjectMock (email)
 *                                              └── AlertService         → @InjectMock (alerts)
 *
 * Payment status routing (PaymentProcessService.processPaymentStatus):
 *   SUCCESS   → enrichOrder + COMPLETED + Odoo + token + email notification
 *   PENDING   → PENDING + Odoo
 *   FAILED    → FAILED, no Odoo call
 *   CANCELLED → CANCELLED + Odoo (only if order exists)
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PaymentProcessIntegrationTest {

    // ============================================================
    // Constants
    // ============================================================

    private static final String REFERENCE_CODE   = "REF-INT-001";
    private static final String EMPLOYER_ID       = "EMP-INT-001";
    private static final String EMPLOYER_EMAIL    = "employer@integration-test.com";
    private static final String COMPANY_NAME      = "Integration Test GmbH";

    // Adjust if @ApplicationPath("/api") is present → "/api/webhooks/appypay"
    private static final String WEBHOOK_PATH      = "/webhooks/appypay";

    // WireMock port must match odoo.url and quarkus.rest-client.odoo-api.url in application-test.yml
    private static final int ODOO_WIREMOCK_PORT   = 9090;
    private static final String ODOO_WEBHOOK_PATH = "/api/webhook/payment";

    // ============================================================
    // Infrastructure
    // ============================================================

    private static WireMockServer wireMock;

    @Inject
    PaymentProcessService paymentProcessService;

    @InjectMock
    NotificationService notificationService;

    @InjectMock
    AlertService alertService;

    @InjectMock
    DocumentTokenService tokenService;

    // ============================================================
    // Lifecycle
    // ============================================================

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(
                WireMockConfiguration.wireMockConfig().port(ODOO_WIREMOCK_PORT)
        );
        wireMock.start();
        WireMock.configureFor("localhost", ODOO_WIREMOCK_PORT);
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMock != null) {
            wireMock.stop();
        }
    }

    @BeforeEach
    @Transactional
    void setUp() {
        wireMock.resetAll();
        Mockito.reset(notificationService, alertService, tokenService);

        cleanDatabase();
        seedEmployerReference();
        stubOdooAcceptAll();
        stubMockedServices();
    }

    // ============================================================
    // SUCCESS flow
    // ============================================================

    @Test
    @Order(1)
    @DisplayName("SUCCESS: webhook accepted, order COMPLETED, Odoo notified, email sent")
    void successfulPayment_completesOrderAndNotifiesEmployer() {
        AppyPayWebhookPayload payload = buildPayload("SUCCESS");

        postWebhook(payload).statusCode(200);
        processPayloadSync(payload);

        awaitOrder(payload.getMerchantTransactionId(), order -> {
            assertEquals("COMPLETED", order.getStatus());
            assertEquals(EMPLOYER_ID,    order.getEmployerId());
            assertEquals(EMPLOYER_EMAIL, order.getEmployerEmail());
            assertEquals(REFERENCE_CODE, order.getReferenceCode());
            assertEquals(PackageType.STANDARD, order.getPackageType());
            assertNotNull(order.getCandidateIds());
            assertFalse(order.getCandidateIds().isEmpty());
            assertNotNull(order.getOdooDocumentIds());
        });

        // Odoo must have received exactly one payment call (REST /api/webhook/payment)
        wireMock.verify(1, postRequestedFor(urlPathEqualTo(ODOO_WEBHOOK_PATH)));

        // Email notification must have been sent to the employer
        verify(notificationService).sendDocumentAccessEmail(
                anyString(), anyString(), any(), anyInt()
        );
    }

    @Test
    @Order(2)
    @DisplayName("SUCCESS: missing employer reference triggers alert, order not completed")
    void successfulPayment_unknownReference_alertSentAndOrderNotCompleted() {
        AppyPayWebhookPayload payload = buildPayload("SUCCESS");
        payload.getReference().setReferenceNumber("UNKNOWN-REF-999");

        postWebhook(payload).statusCode(200);
        processPayloadSync(payload);

        await().atMost(Duration.ofSeconds(10)).pollDelay(Duration.ofSeconds(1)).untilAsserted(() ->
                verify(alertService).sendEmployerReferenceNotFoundAlert(anyString(), anyString())
        );

        // No completed order should exist for this transaction
        List<OrderEntity> orders = OrderEntity.list(
                "merchantTransactionId", payload.getMerchantTransactionId());
        orders.forEach(o -> assertNotEquals("COMPLETED", o.getStatus()));
    }

    @Test
    @Order(3)
    @DisplayName("SUCCESS: token generation failure triggers alert but does not fail the payment")
    void successfulPayment_tokenGenerationFails_alertSentPaymentStillCompleted() {
        // Simulate token service failure
        when(tokenService.generateAccessToken(any()))
                .thenThrow(new RuntimeException("JWT key unavailable"));

        AppyPayWebhookPayload payload = buildPayload("SUCCESS");

        postWebhook(payload).statusCode(200);
        processPayloadSync(payload);

        // Order still reaches COMPLETED — token failure is non-fatal
        awaitOrder(payload.getMerchantTransactionId(), order ->
                assertEquals("COMPLETED", order.getStatus())
        );

        // Admin must be alerted about the token failure
        verify(alertService).sendTokenGenerationAlert(any(), anyString());
    }

    // ============================================================
    // PENDING flow
    // ============================================================

    @Test
    @Order(4)
    @DisplayName("PENDING: order set to PENDING, Odoo notified, no email sent")
    void pendingPayment_orderSetToPendingAndOdooNotified() {
        AppyPayWebhookPayload payload = buildPayload("PENDING");

        postWebhook(payload).statusCode(200);
        processPayloadSync(payload);

        awaitOrder(payload.getMerchantTransactionId(), order ->
                assertEquals("PENDING", order.getStatus())
        );

        // Odoo must be notified for pending payments
        wireMock.verify(1, postRequestedFor(urlPathEqualTo(ODOO_WEBHOOK_PATH)));

        // No email notification for pending payments
        Mockito.verifyNoInteractions(notificationService);
    }

    // ============================================================
    // FAILED flow
    // ============================================================

    @Test
    @Order(5)
    @DisplayName("FAILED: order set to FAILED, Odoo NOT called, no email sent")
    void failedPayment_orderSetToFailedWithoutCallingOdoo() {
        AppyPayWebhookPayload payload = buildPayload("FAILED");

        postWebhook(payload).statusCode(200);
        processPayloadSync(payload);

        awaitOrder(payload.getMerchantTransactionId(), order ->
                assertEquals("FAILED", order.getStatus())
        );

        // Critical: Odoo must NOT be called for failed payments
        wireMock.verify(0, postRequestedFor(urlPathEqualTo(ODOO_WEBHOOK_PATH)));

        // No email notification for failed payments
        Mockito.verifyNoInteractions(notificationService);
    }

    // ============================================================
    // CANCELLED flow
    // ============================================================

    @Test
    @Order(6)
    @DisplayName("CANCELLED: existing order cancelled and Odoo notified")
    void cancelledPayment_existingOrder_cancelledAndOdooNotified() {
        // First create an order via a SUCCESS webhook
        AppyPayWebhookPayload successPayload = buildPayload("SUCCESS");
        postWebhook(successPayload).statusCode(200);
        processPayloadSync(successPayload);
        awaitOrder(successPayload.getMerchantTransactionId(), order ->
                assertEquals("COMPLETED", order.getStatus())
        );

        // Now send a CANCELLED webhook for the same transaction
        AppyPayWebhookPayload cancelPayload = buildPayload("CANCELLED");
        cancelPayload.setMerchantTransactionId(successPayload.getMerchantTransactionId());

        postWebhook(cancelPayload).statusCode(200);
        processPayloadSync(cancelPayload);

        awaitOrder(cancelPayload.getMerchantTransactionId(), order ->
                assertEquals("CANCELLED", order.getStatus())
        );

        // Odoo must be notified about the cancellation
        wireMock.verify(postRequestedFor(urlPathEqualTo(ODOO_WEBHOOK_PATH)));
    }

    @Test
    @Order(7)
    @DisplayName("CANCELLED: no existing order — warning logged, nothing persisted")
    void cancelledPayment_noExistingOrder_gracefullyIgnored() {
        AppyPayWebhookPayload payload = buildPayload("CANCELLED");
        // Use a transaction ID that has no prior order
        payload.setMerchantTransactionId("ORD-NONEXISTENT-" + UUID.randomUUID());

        postWebhook(payload).statusCode(200);
        processPayloadSync(payload);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                QuarkusTransaction.requiringNew().run(() -> {
                    List<OrderEntity> orders = OrderEntity.list(
                            "merchantTransactionId", payload.getMerchantTransactionId());
                    assertTrue(orders.isEmpty(), "No order should be created for unknown cancellation");
                })
        );

        wireMock.verify(0, postRequestedFor(urlPathEqualTo(ODOO_WEBHOOK_PATH)));
    }

    // ============================================================
    // Input validation
    // ============================================================

    @Test
    @Order(8)
    @DisplayName("INVALID: malformed JSON returns 400")
    void invalidJson_returns400() {
        given()
                .contentType(ContentType.JSON)
                .body("{invalid json")
                .when()
                .post(WEBHOOK_PATH)
                .then()
                .statusCode(400);
    }

    @Test
    @Order(9)
    @DisplayName("INVALID: unknown status — no order created")
    void unknownStatus_noOrderCreated() {
        AppyPayWebhookPayload payload = buildPayload("UNKNOWN_STATUS");

        postWebhook(payload).statusCode(200);
        processPayloadSync(payload);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                QuarkusTransaction.requiringNew().run(() -> {
                    List<OrderEntity> orders = OrderEntity.list(
                            "merchantTransactionId", payload.getMerchantTransactionId());
                    assertTrue(orders.isEmpty(), "Unknown status should not create an order");
                })
        );
    }

    // ============================================================
    // Idempotency
    // ============================================================

    @Test
    @Order(10)
    @DisplayName("IDEMPOTENCY: duplicate SUCCESS webhook does not create duplicate orders")
    void duplicateWebhook_idempotent() {
        AppyPayWebhookPayload payload = buildPayload("SUCCESS");

        postWebhook(payload).statusCode(200);
        processPayloadSync(payload);
        processPayloadSync(payload); // Duplicate - should be idempotent via findOrCreate

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                QuarkusTransaction.requiringNew().run(() -> {
                    long count = OrderEntity.count(
                            "merchantTransactionId", payload.getMerchantTransactionId());
                    assertEquals(1, count, "Duplicate webhook must not create duplicate orders");
                })
        );
    }

    // ============================================================
    // Concurrency / throughput
    // ============================================================

    @Test
    @Order(11)
    @DisplayName("THROUGHPUT: three concurrent SUCCESS webhooks all complete independently")
    void multipleWebhooks_allComplete() {
        AppyPayWebhookPayload p1 = buildPayload("SUCCESS", "ORD-MULTI-001", "TXN-MULTI-001");
        AppyPayWebhookPayload p2 = buildPayload("SUCCESS", "ORD-MULTI-002", "TXN-MULTI-002");
        AppyPayWebhookPayload p3 = buildPayload("SUCCESS", "ORD-MULTI-003", "TXN-MULTI-003");

        postWebhook(p1).statusCode(200);
        postWebhook(p2).statusCode(200);
        postWebhook(p3).statusCode(200);
        processPayloadSync(p1);
        processPayloadSync(p2);
        processPayloadSync(p3);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                QuarkusTransaction.requiringNew().run(() -> {
                    long completed = OrderEntity.count("status", "COMPLETED");
                    assertEquals(3, completed, "All three orders must reach COMPLETED status");
                })
        );
    }

    // ============================================================
    // Stub setup
    // ============================================================

    /**
     * Odoo REST webhook stub: OdooPaymentService posts to /api/webhook/payment.
     * Returns success so payment sync completes.
     */
    private void stubOdooAcceptAll() {
        stubFor(post(urlPathEqualTo(ODOO_WEBHOOK_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                { "success": true, "payment_id": 42, "message": "Payment processed" }
                                """)));
    }

    private void stubMockedServices() {
        // NotificationService: email sending is a side effect, not under test here
        doNothing().when(notificationService)
                .sendDocumentAccessEmail(anyString(), anyString(), any(), anyInt());

        // AlertService: alert sending is a side effect, not under test here
        doNothing().when(alertService).sendTokenGenerationAlert(any(), anyString());
        doNothing().when(alertService).sendEmployerReferenceNotFoundAlert(anyString(), anyString());
        doNothing().when(alertService).sendOdooApiFailureAlert(anyString(), anyString());

        // DocumentTokenService: returns deterministic test values
        when(tokenService.generateAccessToken(any())).thenReturn("test-access-token");
        when(tokenService.generateDownloadUrl(anyString()))
                .thenReturn("http://localhost:8081/api/v1/documents/download?token=test-access-token");
    }

    // ============================================================
    // Database helpers
    // ============================================================

    @Transactional
    void cleanDatabase() {
        OrderEntity.deleteAll();
        EmployerReference.deleteAll();
    }

    @Transactional
    void seedEmployerReference() {
        EmployerReference ref = new EmployerReference();
        ref.setReferenceCode(REFERENCE_CODE);
        ref.setEmployerId(EMPLOYER_ID);
        ref.setEmployerEmail(EMPLOYER_EMAIL);
        ref.setCompanyName(COMPANY_NAME);
        ref.persist();
    }

    // ============================================================
    // Payload factory
    // ============================================================

    private AppyPayWebhookPayload buildPayload(String status) {
        String ts = String.valueOf(System.currentTimeMillis());
        return buildPayload(status, "ORD-INT-" + ts, "TXN-INT-" + ts);
    }

    private AppyPayWebhookPayload buildPayload(String status, String merchantTxId, String txId) {
        CustomerInfo customer = new CustomerInfo();
        customer.setName("Integration Test Customer");
        customer.setEmail("customer@integration-test.com");
        customer.setPhone("+244 900 000 000");

        ReferenceInfo reference = new ReferenceInfo();
        reference.setEntity("employer");
        reference.setReferenceNumber(REFERENCE_CODE);

        AppyPayWebhookPayload payload = new AppyPayWebhookPayload();
        payload.setId(txId);
        payload.setMerchantTransactionId(merchantTxId);
        payload.setStatus(status);
        payload.setType("PAYMENT");
        payload.setAmount(new BigDecimal("150.00"));
        payload.setCurrency("AOA");
        payload.setCustomer(customer);
        payload.setReference(reference);

        return payload;
    }

    // ============================================================
    // Request / assertion helpers
    // ============================================================

    private io.restassured.response.ValidatableResponse postWebhook(AppyPayWebhookPayload payload) {
        return given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post(WEBHOOK_PATH)
                .then();
    }

    /**
     * Invoke payment processing synchronously.
     * Bypasses async messaging so tests can verify results immediately.
     * Catches exceptions for tests that expect processing to fail (e.g. unknown reference).
     */
    private void processPayloadSync(AppyPayWebhookPayload payload) {
        try {
            paymentProcessService.processPaymentStatus(payload);
        } catch (RuntimeException e) {
            // Expected for unknown reference, token failures, etc.
        }
    }

    private void awaitOrder(String merchantTransactionId,
                            java.util.function.Consumer<OrderEntity> assertions) {
        await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(500))
                .pollDelay(Duration.ofMillis(500))
                .untilAsserted(() ->
                        // Awaitility polls on a separate thread with no CDI context.
                        // QuarkusTransaction.requiringNew() opens a short-lived transaction
                        // so Panache can access the EntityManager.
                        QuarkusTransaction.requiringNew().run(() -> {
                            List<OrderEntity> orders = OrderEntity.list(
                                    "merchantTransactionId", merchantTransactionId);
                            assertFalse(orders.isEmpty(),
                                    "Order should exist for: " + merchantTransactionId);
                            assertions.accept(orders.get(0));
                        })
                );
    }
}