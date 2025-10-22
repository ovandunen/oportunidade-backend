package ao.co.oportunidade.webhook.resource;

import ao.co.oportunidade.webhook.*;
import ao.co.oportunidade.webhook.dto.AppyPayWebhookPayload;
import ao.co.oportunidade.webhook.dto.CustomerInfo;
import ao.co.oportunidade.webhook.dto.ReferenceInfo;
import ao.co.oportunidade.webhook.entity.PaymentTransactionEntity;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

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

    private final static UUID ORDER_ID = UUID.randomUUID();
    public static final String MERCHANT_TRANSACTION_ID = "ORDER-TEST-123";
    public static final String TRANSACTION_ID = "ORDER-TEST-123";
    public static final String AMOUNT = "1500.00";
    public static final String CURRENCY_AOA = "AOA";
    public static final String CUSTOMER_NAME = "Test Customer";
    public static final String CUSTOMER_EMAIL = "test@example.com";
    public static final String APPYPAY_TRANSACTION_ID = "test-tx-123";
    public static final String REFERENCE = "987654321";

    @Inject
    WebhookEventRepository webhookEventRepository;

    @Inject
    OrderRepository orderRepository;

    @Inject
    PaymentTransactionRepository paymentTransactionRepository;

    @BeforeEach
    @Transactional
    void setUp() {
        final Order order = createOrder();
        orderRepository.createDomain(order);
        // Usage:
        final PaymentTransactionEntity paymentTransaction = createTransaction(APPYPAY_TRANSACTION_ID,
                PaymentTransaction.TransactionStatus.SUCCESS,
                REFERENCE,
                Double.valueOf(AMOUNT),
                Currency.getInstance(CURRENCY_AOA));
                paymentTransaction.setOrderId(ORDER_ID);

        paymentTransactionRepository.persist(paymentTransaction);
    }

    private static Order createOrder() {
        final Order order = Order.builder().id(ORDER_ID).
                merchantTransactionId(MERCHANT_TRANSACTION_ID).
        amount(new BigDecimal(AMOUNT)).build();
        order.setCurrency(CURRENCY_AOA);
        order.setStatus(Order.OrderStatus.PAID);
        order.setCustomerName(CUSTOMER_NAME);
        order.setCustomerEmail(CUSTOMER_EMAIL);
        return order;
    }

    public static PaymentTransactionEntity createTransaction(final String appyPayTransactionId,
                                                             final PaymentTransaction.TransactionStatus status,
                                                             final String referenceNumber,
                                                             final Double amount,
                                                             final Currency currency) {
        final PaymentTransactionEntity transaction = new PaymentTransactionEntity();
        transaction.setAppypayTransactionId(appyPayTransactionId);
        transaction.setStatus(status.toString());
        transaction.setReferenceNumber(referenceNumber);
        transaction.setAmount(BigDecimal.valueOf(amount));
        transaction.setCurrency(currency.getCurrencyCode());
        transaction.setTransactionDate(Instant.now());
        transaction.setCreatedDate(Instant.now());
        return transaction;
    }



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
        final AppyPayWebhookPayload payload = createSuccessPayload();

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
        final AppyPayWebhookPayload payload = createSuccessPayload();

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
        final List<WebhookEvent> events = webhookEventRepository.findByAppyPayTransactionId("test-tx-123")
                .stream().toList();
        
        assertThat(events).isNotEmpty();
        WebhookEvent event = events.getFirst();
        assertThat(event.getAppypayTransactionId()).isEqualTo("test-tx-123");
        assertThat(event.getMerchantTransactionId()).isEqualTo("ORDER-TEST-123");
        assertThat(event.getWebhookType()).isEqualTo("Charge");
    }

    @Test
    @Transactional
    void testReceiveWebhook_CreatesOrderAndTransaction() throws InterruptedException {
        // Given
        final AppyPayWebhookPayload payload = createSuccessPayload();

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
        final List<Order> orders = orderRepository.findByMerchantTransactionId(MERCHANT_TRANSACTION_ID)
                .stream().toList();
        
        assertThat(orders).isNotEmpty();
        final Order order = orders.getFirst();
        assertThat(order.getMerchantTransactionId()).isEqualTo(TRANSACTION_ID);
        assertThat(order.getAmount()).isEqualByComparingTo(new BigDecimal(AMOUNT));
        assertThat(order.getCurrency()).isEqualTo(CURRENCY_AOA);
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.PAID);
        assertThat(order.getCustomerName()).isEqualTo(CUSTOMER_NAME);
        assertThat(order.getCustomerEmail()).isEqualTo(CUSTOMER_EMAIL);

        // Then - verify transaction was created
        final List<PaymentTransaction> transactions = paymentTransactionRepository
                .findByAppyPayTransactionId(APPYPAY_TRANSACTION_ID)
                .stream().toList();
        
        assertThat(transactions).isNotEmpty();
        final PaymentTransaction tx = transactions.getFirst();
        assertThat(tx.getAppypayTransactionId()).isEqualTo(APPYPAY_TRANSACTION_ID);
        assertThat(tx.getStatus()).isEqualTo(PaymentTransaction.TransactionStatus.SUCCESS);
        assertThat(tx.getReferenceNumber()).isEqualTo(REFERENCE);
    }

    @Test
    void testReceiveWebhook_Idempotency() throws InterruptedException {
        // Given
        final AppyPayWebhookPayload payload = createSuccessPayload();
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
        
        final long orderCount = orderRepository.count("merchantTransactionId", "ORDER-TEST-123");
        assertThat(orderCount).isEqualTo(1);

        final long txCount = paymentTransactionRepository.count("appypayTransactionId", "test-tx-123");
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
        final AppyPayWebhookPayload payload = createPendingPayload();

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
        final List<Order> orders = orderRepository.findByMerchantTransactionId("ORDER-PENDING-456")
                .stream().toList();
        
        assertThat(orders).isNotEmpty();
        final Order order = orders.get(0);
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.PENDING);

        final List<PaymentTransaction> transactions = paymentTransactionRepository
                .findByAppyPayTransactionId("test-tx-pending-456")
                .stream().toList();
        
        assertThat(transactions).isNotEmpty();
        final PaymentTransaction tx = transactions.get(0);
        assertThat(tx.getStatus()).isEqualTo(PaymentTransaction.TransactionStatus.PENDING);
    }

    @Test
    @Transactional
    void testReceiveWebhook_FailedStatus() throws InterruptedException {
        // Given
        final AppyPayWebhookPayload payload = createFailedPayload();

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
        final List<Order> orders = orderRepository.findByMerchantTransactionId("ORDER-FAILED-789")
                .stream().toList();
        
        assertThat(orders).isNotEmpty();
        final Order order = orders.getFirst();
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.FAILED);

        final List<PaymentTransaction> transactions = paymentTransactionRepository
                .findByAppyPayTransactionId("test-tx-failed-789")
                .stream().toList();
        
        assertThat(transactions).isNotEmpty();
        final PaymentTransaction tx = transactions.get(0);
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
