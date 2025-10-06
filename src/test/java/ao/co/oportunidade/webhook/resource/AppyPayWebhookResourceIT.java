package ao.co.oportunidade.webhook.resource;

import ao.co.oportunidade.webhook.dto.AppyPayWebhookPayload;
import ao.co.oportunidade.webhook.dto.CustomerInfo;
import ao.co.oportunidade.webhook.dto.ReferenceInfo;
import ao.co.oportunidade.webhook.dto.WebhookResponse;
import ao.co.oportunidade.webhook.entity.Order;
import ao.co.oportunidade.webhook.entity.OrderRepository;
import ao.co.oportunidade.webhook.entity.PaymentTransaction;
import ao.co.oportunidade.webhook.entity.PaymentTransactionRepository;
import ao.co.oportunidade.webhook.entity.WebhookEvent;
import ao.co.oportunidade.webhook.entity.WebhookEventRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

/**
 * Integration tests for AppyPay webhook endpoint.
 * Tests the complete flow from HTTP request to database persistence.
 */
@QuarkusTest
class AppyPayWebhookResourceIT {

    @Inject
    WebhookEventRepository webhookEventRepository;

    @Inject
    OrderRepository orderRepository;

    @Inject
    PaymentTransactionRepository paymentTransactionRepository;

    @AfterEach
    @Transactional
    void cleanUp() {
        paymentTransactionRepository.deleteAll();
        orderRepository.deleteAll();
        webhookEventRepository.deleteAll();
    }

    @Test
    void testReceiveWebhook_Success_ReturnsOk() {
        // Given
        AppyPayWebhookPayload payload = createSuccessPayload();

        // When/Then
        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/webhooks/appypay")
                .then()
                .statusCode(200)
                .body("status", is("received"))
                .body("message", is("Webhook received and queued for processing"))
                .body("eventId", notNullValue());
    }

    @Test
    @Transactional
    void testReceiveWebhook_CreatesWebhookEvent() throws InterruptedException {
        // Given
        AppyPayWebhookPayload payload = createSuccessPayload();

        // When
        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/webhooks/appypay")
                .then()
                .statusCode(200);

        // Give async processing time to complete
        Thread.sleep(2000);

        // Then
        List<WebhookEvent> events = webhookEventRepository.findByAppyPayTransactionId("test-tx-123")
                .stream().toList();
        
        assertThat(events).isNotEmpty();
        WebhookEvent event = events.get(0);
        assertThat(event.getAppypayTransactionId()).isEqualTo("test-tx-123");
        assertThat(event.getMerchantTransactionId()).isEqualTo("ORDER-TEST-123");
        assertThat(event.getWebhookType()).isEqualTo("Charge");
    }

    @Test
    @Transactional
    void testReceiveWebhook_CreatesOrderAndTransaction() throws InterruptedException {
        // Given
        AppyPayWebhookPayload payload = createSuccessPayload();

        // When
        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/webhooks/appypay")
                .then()
                .statusCode(200);

        // Give async processing time to complete
        Thread.sleep(2000);

        // Then - verify order was created
        List<Order> orders = orderRepository.findByMerchantTransactionId("ORDER-TEST-123")
                .stream().toList();
        
        assertThat(orders).isNotEmpty();
        Order order = orders.get(0);
        assertThat(order.getMerchantTransactionId()).isEqualTo("ORDER-TEST-123");
        assertThat(order.getAmount()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(order.getCurrency()).isEqualTo("AOA");
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.PAID);
        assertThat(order.getCustomerName()).isEqualTo("Test Customer");
        assertThat(order.getCustomerEmail()).isEqualTo("test@example.com");

        // Then - verify transaction was created
        List<PaymentTransaction> transactions = paymentTransactionRepository
                .findByAppyPayTransactionId("test-tx-123")
                .stream().toList();
        
        assertThat(transactions).isNotEmpty();
        PaymentTransaction tx = transactions.get(0);
        assertThat(tx.getAppypayTransactionId()).isEqualTo("test-tx-123");
        assertThat(tx.getStatus()).isEqualTo(PaymentTransaction.TransactionStatus.SUCCESS);
        assertThat(tx.getReferenceNumber()).isEqualTo("987654321");
    }

    @Test
    void testReceiveWebhook_Idempotency() throws InterruptedException {
        // Given
        AppyPayWebhookPayload payload = createSuccessPayload();

        // When - send same webhook twice
        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/webhooks/appypay")
                .then()
                .statusCode(200);

        Thread.sleep(2000); // Wait for processing

        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/webhooks/appypay")
                .then()
                .statusCode(200)
                .body("status", is("already_processed"))
                .body("message", is("Webhook already processed"));

        // Then - verify only one order and transaction was created
        Thread.sleep(1000);
        
        long orderCount = orderRepository.count("merchantTransactionId", "ORDER-TEST-123");
        assertThat(orderCount).isEqualTo(1);

        long txCount = paymentTransactionRepository.count("appypayTransactionId", "test-tx-123");
        assertThat(txCount).isEqualTo(1);
    }

    @Test
    void testHealthEndpoint() {
        given()
                .when()
                .get("/webhooks/appypay/health")
                .then()
                .statusCode(200)
                .body("status", is("UP"))
                .body("service", is("AppyPay Webhook"));
    }

    @Test
    @Transactional
    void testReceiveWebhook_PendingStatus() throws InterruptedException {
        // Given
        AppyPayWebhookPayload payload = createPendingPayload();

        // When
        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/webhooks/appypay")
                .then()
                .statusCode(200);

        Thread.sleep(2000);

        // Then
        List<Order> orders = orderRepository.findByMerchantTransactionId("ORDER-PENDING-456")
                .stream().toList();
        
        assertThat(orders).isNotEmpty();
        Order order = orders.get(0);
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.PENDING);

        List<PaymentTransaction> transactions = paymentTransactionRepository
                .findByAppyPayTransactionId("test-tx-pending-456")
                .stream().toList();
        
        assertThat(transactions).isNotEmpty();
        PaymentTransaction tx = transactions.get(0);
        assertThat(tx.getStatus()).isEqualTo(PaymentTransaction.TransactionStatus.PENDING);
    }

    @Test
    @Transactional
    void testReceiveWebhook_FailedStatus() throws InterruptedException {
        // Given
        AppyPayWebhookPayload payload = createFailedPayload();

        // When
        given()
                .contentType(ContentType.JSON)
                .body(payload)
                .when()
                .post("/webhooks/appypay")
                .then()
                .statusCode(200);

        Thread.sleep(2000);

        // Then
        List<Order> orders = orderRepository.findByMerchantTransactionId("ORDER-FAILED-789")
                .stream().toList();
        
        assertThat(orders).isNotEmpty();
        Order order = orders.get(0);
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.FAILED);

        List<PaymentTransaction> transactions = paymentTransactionRepository
                .findByAppyPayTransactionId("test-tx-failed-789")
                .stream().toList();
        
        assertThat(transactions).isNotEmpty();
        PaymentTransaction tx = transactions.get(0);
        assertThat(tx.getStatus()).isEqualTo(PaymentTransaction.TransactionStatus.FAILED);
    }

    // Helper methods to create test payloads
    private AppyPayWebhookPayload createSuccessPayload() {
        return AppyPayWebhookPayload.builder()
                .id("test-tx-123")
                .merchantTransactionId("ORDER-TEST-123")
                .type("Charge")
                .amount(new BigDecimal("1500.00"))
                .currency("AOA")
                .status("Success")
                .paymentMethod("REF")
                .reference(ReferenceInfo.builder()
                        .referenceNumber("987654321")
                        .entity("00123")
                        .dueDate(Instant.now().plusSeconds(86400))
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

    private AppyPayWebhookPayload createPendingPayload() {
        return AppyPayWebhookPayload.builder()
                .id("test-tx-pending-456")
                .merchantTransactionId("ORDER-PENDING-456")
                .type("Charge")
                .amount(new BigDecimal("2000.00"))
                .currency("AOA")
                .status("Pending")
                .paymentMethod("REF")
                .reference(ReferenceInfo.builder()
                        .referenceNumber("111222333")
                        .entity("00123")
                        .dueDate(Instant.now().plusSeconds(86400))
                        .build())
                .customer(CustomerInfo.builder()
                        .name("Pending Customer")
                        .email("pending@example.com")
                        .phone("+244900000001")
                        .build())
                .createdDate(Instant.now())
                .updatedDate(Instant.now())
                .build();
    }

    private AppyPayWebhookPayload createFailedPayload() {
        return AppyPayWebhookPayload.builder()
                .id("test-tx-failed-789")
                .merchantTransactionId("ORDER-FAILED-789")
                .type("Charge")
                .amount(new BigDecimal("3000.00"))
                .currency("AOA")
                .status("Failed")
                .paymentMethod("REF")
                .reference(ReferenceInfo.builder()
                        .referenceNumber("444555666")
                        .entity("00123")
                        .dueDate(Instant.now().plusSeconds(86400))
                        .build())
                .customer(CustomerInfo.builder()
                        .name("Failed Customer")
                        .email("failed@example.com")
                        .phone("+244900000002")
                        .build())
                .createdDate(Instant.now())
                .updatedDate(Instant.now())
                .build();
    }
}
