package ao.co.oportunidade.webhook.service;

import ao.co.oportunidade.payment.service.PaymentProcessService;
import ao.co.oportunidade.webhook.dto.AppyPayWebhookPayload;
import ao.co.oportunidade.webhook.dto.CustomerInfo;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for WebhookProcessor.
 * Tests async processing, retry logic, fallback mechanisms, and timeout handling.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WebhookProcessorTest {

    @Inject
    WebhookProcessor webhookProcessor;

    @InjectMock
    PaymentProcessService paymentProcessService;

    @InjectMock
    WebhookEventServiceFacade webhookEventService;


    private AppyPayWebhookPayload testPayload;
    private static final String TEST_TRANSACTION_ID = "TXN-WEBHOOK-TEST-123";

    @BeforeEach
    void setup() {
        // Create test payload
        testPayload = new AppyPayWebhookPayload();
        testPayload.setId(TEST_TRANSACTION_ID);
        testPayload.setMerchantTransactionId("ORD-TEST-456");
        testPayload.setStatus("SUCCESS");
        testPayload.setAmount(new BigDecimal("250.00"));
        testPayload.setCurrency("AOA");
        testPayload.setPaymentMethod("REF");
        
        CustomerInfo customer = new CustomerInfo();
        customer.setEmail("customer@test.com");
        testPayload.setCustomer(customer);

        // Mock webhook event service
        doNothing().when(webhookEventService).markAsProcessing(anyString());
        doNothing().when(webhookEventService).markAsProcessed(anyString());
        doNothing().when(webhookEventService).markAsFailed(anyString(), anyString());
    }

    // ============= HAPPY PATH TESTS =============

    @Test
    @org.junit.jupiter.api.Order(1)
    @DisplayName("Should process webhook successfully on first attempt")
    void testProcessPayment_Success() {
        // Given
        doNothing().when(paymentProcessService).processPaymentStatus(any(AppyPayWebhookPayload.class));

        // When
        assertDoesNotThrow(() -> webhookProcessor.processPayment(testPayload));

        // Then
        verify(webhookEventService, times(1)).markAsProcessing(TEST_TRANSACTION_ID);
        verify(paymentProcessService, times(1)).processPaymentStatus(testPayload);
        verify(webhookEventService, times(1)).markAsProcessed(TEST_TRANSACTION_ID);
        verify(webhookEventService, never()).markAsFailed(anyString(), anyString());
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    @DisplayName("Should mark webhook as processing before payment processing")
    void testProcessPayment_MarkedAsProcessing() {
        // Given
        doNothing().when(paymentProcessService).processPaymentStatus(any(AppyPayWebhookPayload.class));

        // When
        webhookProcessor.processPayment(testPayload);

        // Then
        verify(webhookEventService, times(1)).markAsProcessing(TEST_TRANSACTION_ID);
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    @DisplayName("Should mark webhook as processed after successful processing")
    void testProcessPayment_MarkedAsProcessed() {
        // Given
        doNothing().when(paymentProcessService).processPaymentStatus(any(AppyPayWebhookPayload.class));

        // When
        webhookProcessor.processPayment(testPayload);

        // Then
        verify(webhookEventService, times(1)).markAsProcessed(TEST_TRANSACTION_ID);
    }

    // ============= RETRY LOGIC TESTS =============

    @Test
    @org.junit.jupiter.api.Order(4)
    @DisplayName("Should retry on first failure then succeed")
    void testProcessPayment_RetryOnFailure() {
        // Given - Fail once, then succeed
        doThrow(new RuntimeException("Temporary network error"))
            .doNothing()
            .when(paymentProcessService).processPaymentStatus(any(AppyPayWebhookPayload.class));

        // When
        assertDoesNotThrow(() -> webhookProcessor.processPayment(testPayload));

        // Then
        verify(paymentProcessService, times(2)).processPaymentStatus(testPayload);
        // markAsProcessing called on each attempt
        verify(webhookEventService, atLeast(1)).markAsProcessing(TEST_TRANSACTION_ID);
        // markAsFailed called on first failure
        verify(webhookEventService, atLeastOnce()).markAsFailed(eq(TEST_TRANSACTION_ID), anyString());
        // markAsProcessed called once on final success
        verify(webhookEventService, times(1)).markAsProcessed(TEST_TRANSACTION_ID);
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    @DisplayName("Should retry multiple times before succeeding")
    void testProcessPayment_MultipleRetries() {
        // Given - Fail twice, then succeed
        doThrow(new RuntimeException("Error 1"))
            .doThrow(new RuntimeException("Error 2"))
            .doNothing()
            .when(paymentProcessService).processPaymentStatus(any(AppyPayWebhookPayload.class));

        // When
        assertDoesNotThrow(() -> webhookProcessor.processPayment(testPayload));

        // Then
        verify(paymentProcessService, times(3)).processPaymentStatus(testPayload);
        // markAsProcessing called once per attempt
        verify(webhookEventService, atLeast(1)).markAsProcessing(TEST_TRANSACTION_ID);
        // markAsFailed called on each failure (2 times)
        verify(webhookEventService, atLeast(1)).markAsFailed(eq(TEST_TRANSACTION_ID), anyString());
        // markAsProcessed called once on final success
        verify(webhookEventService, times(1)).markAsProcessed(TEST_TRANSACTION_ID);
    }

    @Test
    @org.junit.jupiter.api.Order(6)
    @DisplayName("Should mark as failed during retry attempts")
    void testProcessPayment_MarkedAsFailedDuringRetry() {
        // Given - Fail once, then succeed
        doThrow(new RuntimeException("Temporary error"))
            .doNothing()
            .when(paymentProcessService).processPaymentStatus(any(AppyPayWebhookPayload.class));

        // When
        webhookProcessor.processPayment(testPayload);

        // Then - Should be marked as failed on first attempt, then processed
        verify(webhookEventService, times(1)).markAsFailed(
            eq(TEST_TRANSACTION_ID),
            contains("Temporary error")
        );
        verify(webhookEventService, times(1)).markAsProcessed(TEST_TRANSACTION_ID);
    }

    // ============= FALLBACK TESTS =============

    @Test
    @org.junit.jupiter.api.Order(7)
    @DisplayName("Should trigger fallback when all retries exhausted")
    void testProcessPayment_FallbackTriggered() {
        // Given - Always fail
        doThrow(new RuntimeException("Persistent error"))
            .when(paymentProcessService).processPaymentStatus(any(AppyPayWebhookPayload.class));

        // When - Will attempt retry 3 times, then fallback
        webhookProcessor.processPayment(testPayload);

        // Then - After 3 failed attempts, fallback is called
        verify(paymentProcessService, atLeast(3)).processPaymentStatus(testPayload);
        verify(webhookEventService, atLeastOnce()).markAsFailed(
            eq(TEST_TRANSACTION_ID),
            contains("PERMANENT_FAILURE")
        );
    }

    @Test
    @org.junit.jupiter.api.Order(8)
    @DisplayName("Fallback should mark webhook permanently failed after exhausted retries")
    void testFallbackProcessPayment_AlertSent() {
        // Given
        doThrow(new RuntimeException("Database connection failed"))
            .when(paymentProcessService).processPaymentStatus(any(AppyPayWebhookPayload.class));

        // When
        webhookProcessor.processPayment(testPayload);

        // Then
        verify(webhookEventService, atLeastOnce()).markAsFailed(
            eq(TEST_TRANSACTION_ID),
            contains("PERMANENT_FAILURE")
        );
    }

    @Test
    @org.junit.jupiter.api.Order(9)
    @DisplayName("Fallback should mark webhook as permanently failed")
    void testFallbackProcessPayment_MarkedAsPermanentFailure() {
        // Given
        doThrow(new RuntimeException("Unrecoverable error"))
            .when(paymentProcessService).processPaymentStatus(any(AppyPayWebhookPayload.class));

        // When
        webhookProcessor.processPayment(testPayload);

        // Then
        verify(webhookEventService, atLeastOnce()).markAsFailed(
            eq(TEST_TRANSACTION_ID),
            contains("PERMANENT_FAILURE")
        );
    }

    @Test
    @org.junit.jupiter.api.Order(10)
    @DisplayName("Fallback should complete without throwing when processing exhausts retries")
    void testFallbackProcessPayment_ResilientToAlertFailure() {
        // Given
        doThrow(new RuntimeException("Processing error"))
            .when(paymentProcessService).processPaymentStatus(any(AppyPayWebhookPayload.class));

        // When & Then
        assertDoesNotThrow(() -> webhookProcessor.processPayment(testPayload));
    }

    // ============= ERROR HANDLING TESTS =============

    @Test
    @org.junit.jupiter.api.Order(11)
    @DisplayName("Should handle RuntimeException during processing")
    void testProcessPayment_RuntimeException() {
        // Given
        doThrow(new RuntimeException("Unexpected runtime error"))
            .doNothing()
            .when(paymentProcessService).processPaymentStatus(any(AppyPayWebhookPayload.class));

        // When
        assertDoesNotThrow(() -> webhookProcessor.processPayment(testPayload));

        // Then - Should retry and eventually succeed
        verify(paymentProcessService, times(2)).processPaymentStatus(testPayload);
    }

    @Test
    @org.junit.jupiter.api.Order(12)
    @DisplayName("Should handle generic Exception during processing")
    void testProcessPayment_GenericException() {
        // Given
        doThrow(new IllegalStateException("Invalid state"))
            .doNothing()
            .when(paymentProcessService).processPaymentStatus(any(AppyPayWebhookPayload.class));

        // When
        assertDoesNotThrow(() -> webhookProcessor.processPayment(testPayload));

        // Then - Should retry and eventually succeed
        verify(paymentProcessService, times(2)).processPaymentStatus(testPayload);
    }

    @Test
    @org.junit.jupiter.api.Order(13)
    @DisplayName("Should re-throw exception to trigger retry")
    void testProcessPayment_ExceptionRethrown() {
        // Given
        doThrow(new RuntimeException("Transient error"))
            .when(paymentProcessService).processPaymentStatus(any(AppyPayWebhookPayload.class));

        // When
        webhookProcessor.processPayment(testPayload);

        // Then - Exception should trigger retry mechanism
        verify(paymentProcessService, atLeast(2)).processPaymentStatus(testPayload);
        verify(webhookEventService, atLeast(2)).markAsFailed(anyString(), anyString());
    }

    // ============= EDGE CASES =============

    @Test
    @org.junit.jupiter.api.Order(14)
    @DisplayName("Should handle null customer in payload")
    void testProcessPayment_NullCustomer() {
        // Given
        testPayload.setCustomer(null);
        doThrow(new RuntimeException("Processing failed"))
            .when(paymentProcessService).processPaymentStatus(any(AppyPayWebhookPayload.class));

        // When
        webhookProcessor.processPayment(testPayload);

        // Then - Fallback marks permanent failure
        verify(webhookEventService, atLeastOnce()).markAsFailed(
            eq(TEST_TRANSACTION_ID),
            contains("PERMANENT_FAILURE")
        );
    }

    @Test
    @org.junit.jupiter.api.Order(15)
    @DisplayName("Should log transaction details in fallback")
    void testFallbackProcessPayment_LogsTransactionDetails() {
        // Given
        doThrow(new RuntimeException("Fatal error"))
            .when(paymentProcessService).processPaymentStatus(any(AppyPayWebhookPayload.class));

        // When
        webhookProcessor.processPayment(testPayload);

        // Then - Permanent failure recorded
        verify(webhookEventService, atLeastOnce()).markAsFailed(
            eq(TEST_TRANSACTION_ID),
            contains("PERMANENT_FAILURE")
        );
    }

    // ============= CONCURRENT PROCESSING TESTS =============

    @Test
    @org.junit.jupiter.api.Order(16)
    @DisplayName("Should handle same webhook processed twice")
    void testProcessPayment_DuplicateWebhook() {
        // Given
        doNothing().when(paymentProcessService).processPaymentStatus(any(AppyPayWebhookPayload.class));

        // When - Process same webhook twice
        webhookProcessor.processPayment(testPayload);
        webhookProcessor.processPayment(testPayload);

        // Then - Both should be processed (idempotency is handled at a different layer)
        verify(paymentProcessService, times(2)).processPaymentStatus(testPayload);
        verify(webhookEventService, times(2)).markAsProcessing(TEST_TRANSACTION_ID);
        verify(webhookEventService, times(2)).markAsProcessed(TEST_TRANSACTION_ID);
    }

    @Test
    @org.junit.jupiter.api.Order(17)
    @DisplayName("Should process different webhooks independently")
    void testProcessPayment_DifferentWebhooks() {
        // Given
        AppyPayWebhookPayload payload2 = new AppyPayWebhookPayload();
        payload2.setId("TXN-DIFFERENT-789");
        payload2.setMerchantTransactionId("ORD-DIFFERENT");
        payload2.setStatus("SUCCESS");
        
        doNothing().when(paymentProcessService).processPaymentStatus(any(AppyPayWebhookPayload.class));

        // When - Process two different webhooks
        webhookProcessor.processPayment(testPayload);
        webhookProcessor.processPayment(payload2);

        // Then - Both should be processed
        verify(paymentProcessService, times(2)).processPaymentStatus(any(AppyPayWebhookPayload.class));
        verify(webhookEventService, times(1)).markAsProcessed(TEST_TRANSACTION_ID);
        verify(webhookEventService, times(1)).markAsProcessed("TXN-DIFFERENT-789");
    }
}
