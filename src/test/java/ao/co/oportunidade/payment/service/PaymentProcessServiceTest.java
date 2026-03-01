package ao.co.oportunidade.payment.service;

import ao.co.oportunidade.employer.model.EmployerReference;
import ao.co.oportunidade.notification.service.AlertService;
import ao.co.oportunidade.order.model.Order;
import ao.co.oportunidade.order.model.PackageType;
import ao.co.oportunidade.order.service.OrderService;
import ao.co.oportunidade.webhook.dto.AppyPayWebhookPayload;
import ao.co.oportunidade.webhook.dto.ReferenceInfo;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.mockito.ArgumentCaptor;
import solutions.envision.odoo.document.service.DocumentTokenService;
import solutions.envision.odoo.service.OdooApiClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for PaymentProcessService.
 * Tests retry logic, enrichment, alert integration, and all payment statuses.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PaymentProcessServiceTest {

    @Inject
    PaymentProcessService paymentProcessService;

    @InjectMock
    AlertService alertService;

    @InjectMock
    @RestClient
    OdooApiClient odooApiClient;

    @InjectMock
    DocumentTokenService tokenService;

    @InjectMock
    NotificationService notificationService;

    @InjectMock
    OrderService orderService;

    private AppyPayWebhookPayload payload;
    private Order testOrder;
    private static final String TEST_TRANSACTION_ID = "TXN-TEST-12345";
    private static final String TEST_REFERENCE_CODE = "TEST-REF-001";

    @BeforeEach
    @Transactional
    void setup() {
        // Reset all mocks
        reset(orderService, odooApiClient, tokenService, notificationService, alertService);

        // Clean up test data
        EmployerReference.deleteAll();

        // Create test employer reference
        EmployerReference employerRef = new EmployerReference();
        employerRef.setReferenceCode(TEST_REFERENCE_CODE);
        employerRef.setEmployerId("EMP-001");
        employerRef.setEmployerEmail("employer@test.com");
        employerRef.setCompanyName("Test Company");
        employerRef.persist();

        // Create test order
        testOrder = new Order();
        testOrder.setId(UUID.randomUUID());
        testOrder.setMerchantTransactionId("ORD-TEST-12345");
        testOrder.setAmount(new BigDecimal("100.00"));
        testOrder.setCurrency("AOA");
        testOrder.setStatus(Order.OrderStatus.PENDING);

        // Create test payload
        payload = new AppyPayWebhookPayload();
        payload.setId(TEST_TRANSACTION_ID);
        payload.setMerchantTransactionId("ORD-TEST-12345");
        payload.setStatus("SUCCESS");
        payload.setAmount(new BigDecimal("100.00"));
        payload.setCurrency("AOA");
        payload.setPaymentMethod("REF");

        ReferenceInfo refInfo = new ReferenceInfo();
        refInfo.setEntity("employer");
        refInfo.setReferenceNumber(TEST_REFERENCE_CODE);
        payload.setReference(refInfo);

        // Mock order service (PaymentProcessService uses findOrCreateFromWebhook for SUCCESS, PENDING, FAILED)
        when(orderService.findOrCreateFromWebhook(any(AppyPayWebhookPayload.class), any(Order.OrderStatus.class)))
                .thenReturn(testOrder);
        when(orderService.find(any(AppyPayWebhookPayload.class))).thenReturn(testOrder);
        doNothing().when(orderService).transact(any(Order.class));
        when(orderService.findByMerchantTransactionId(anyString())).thenReturn(Optional.of(testOrder));

        // Mock Odoo REST client to avoid actual HTTP calls
        when(odooApiClient.sendPayment(anyString(), any())).thenReturn(mockOdooResponse());

        // Mock token generation
        when(tokenService.generateAccessToken(any(Order.class))).thenReturn("test.jwt.token");
        when(tokenService.generateDownloadUrl(anyString())).thenReturn("http://test.com/download?token=test");

        // Mock notifications
        doNothing().when(notificationService).sendDocumentAccessEmail(anyString(), anyString(), any(PackageType.class), anyInt());
        doNothing().when(alertService).sendEmployerReferenceNotFoundAlert(anyString(), anyString());
        doNothing().when(alertService).sendOdooApiFailureAlert(anyString(), anyString());
    }

    private solutions.envision.odoo.dto.OdooWebhookResponse mockOdooResponse() {
        solutions.envision.odoo.dto.OdooWebhookResponse response = new solutions.envision.odoo.dto.OdooWebhookResponse();
        response.setSuccess(true);
        response.setMessage("Payment received");
        response.setPaymentId(12345);
        return response;
    }

    // ============= HAPPY PATH TESTS =============

    @Test
    @org.junit.jupiter.api.Order(1)
    @DisplayName("Should process successful payment with token generation")
    void testProcessPaymentStatus_Success() throws Exception {
        // Given
        when(tokenService.generateAccessToken(any(Order.class))).thenReturn("test.jwt.token");
        when(tokenService.generateDownloadUrl(anyString())).thenReturn("http://localhost:8080/api/documents/download?token=test.jwt.token");
        when(odooApiClient.sendPayment(anyString(), any())).thenReturn(mockOdooResponse());

        // When
        paymentProcessService.processPaymentStatus(payload);

        // Then
        verify(orderService, times(1)).transact(argThat(order ->
                order.getStatus() == Order.OrderStatus.COMPLETED &&
                        "EMP-001".equals(order.getEmployerId()) &&
                        "employer@test.com".equals(order.getEmployerEmail())
        ));
        verify(tokenService, times(1)).generateAccessToken(any(Order.class));
        verify(notificationService, times(1)).sendDocumentAccessEmail(
                eq("employer@test.com"),
                anyString(),
                any(PackageType.class),
                anyInt()
        );
        verify(odooApiClient, times(1)).sendPayment(anyString(), any());
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    @DisplayName("Should enrich order with employer information")
    void testEnrichOrderWithEmployerInfo() throws Exception {
        // When
        paymentProcessService.processPaymentStatus(payload);

        // Then
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderService).transact(orderCaptor.capture());

        Order enrichedOrder = orderCaptor.getValue();
        assertNotNull(enrichedOrder);
        assertEquals("EMP-001", enrichedOrder.getEmployerId());
        assertEquals("employer@test.com", enrichedOrder.getEmployerEmail());
        assertEquals(TEST_REFERENCE_CODE, enrichedOrder.getReferenceCode());
        assertNotNull(enrichedOrder.getPackageType());
        assertNotNull(enrichedOrder.getCandidateIds());
        assertNotNull(enrichedOrder.getOdooDocumentIds());
        assertFalse(enrichedOrder.getCandidateIds().isEmpty());
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    @DisplayName("Should handle pending payment status")
    void testHandlePendingPayment() throws Exception {
        // Given
        payload.setStatus("PENDING");

        // When
        paymentProcessService.processPaymentStatus(payload);

        // Then
        verify(odooApiClient, times(1)).sendPayment(anyString(), any());
        // Token should NOT be generated for pending payments
        verify(tokenService, never()).generateAccessToken(any(Order.class));
        verify(notificationService, never()).sendDocumentAccessEmail(anyString(), anyString(), any(), anyInt());
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    @DisplayName("Should handle failed payment status")
    void testHandleFailedPayment() throws Exception {
        // Given
        payload.setStatus("FAILED");

        // When
        paymentProcessService.processPaymentStatus(payload);

        // Then — FAILED payments must NOT notify Odoo (bug fixed in PaymentProcessService)
        verify(odooApiClient, never()).sendPayment(anyString(), any());
        verify(tokenService, never()).generateAccessToken(any(Order.class));
        verify(notificationService, never()).sendDocumentAccessEmail(anyString(), anyString(), any(), anyInt());
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    @DisplayName("Should handle cancelled payment status")
    void testHandleCancelledPayment() throws Exception {
        // Given
        payload.setStatus("CANCELLED");

        // Need to mock findByMerchantTransactionId since cancelled payment checks for existing order
        when(orderService.findByMerchantTransactionId(payload.getMerchantTransactionId()))
                .thenReturn(Optional.of(testOrder));

        // When
        paymentProcessService.processPaymentStatus(payload);

        // Then
        verify(orderService, times(1)).findByMerchantTransactionId(payload.getMerchantTransactionId());
        verify(odooApiClient, times(1)).sendPayment(anyString(), any());
        verify(tokenService, never()).generateAccessToken(any(Order.class));
        verify(notificationService, never()).sendDocumentAccessEmail(anyString(), anyString(), any(), anyInt());
    }

    // ============= RETRY LOGIC TESTS =============

    @Test
    @org.junit.jupiter.api.Order(6)
    @DisplayName("Should retry Odoo API call on first failure then succeed")
    void testSendToOdooWithRetry_SecondAttemptSuccess() throws Exception {
        // Given
        when(tokenService.generateAccessToken(any(Order.class))).thenReturn("test.jwt.token");
        when(tokenService.generateDownloadUrl(anyString())).thenReturn("http://test.com");

        // Fail once, then succeed
        when(odooApiClient.sendPayment(anyString(), any()))
                .thenThrow(new RuntimeException("Network error"))
                .thenReturn(mockOdooResponse());

        // When
        paymentProcessService.processPaymentStatus(payload);

        // Then
        verify(odooApiClient, times(2)).sendPayment(anyString(), any());
        verify(alertService, atLeastOnce()).sendOdooApiFailureAlert(eq("sendPaymentToOdoo"), anyString());
    }

    @Test
    @org.junit.jupiter.api.Order(7)
    @DisplayName("Should succeed on first Odoo API attempt (no retry)")
    void testSendToOdooWithRetry_FirstAttemptSuccess() throws Exception {
        // Given
        when(tokenService.generateAccessToken(any(Order.class))).thenReturn("test.jwt.token");
        when(tokenService.generateDownloadUrl(anyString())).thenReturn("http://test.com");
        when(odooApiClient.sendPayment(anyString(), any())).thenReturn(mockOdooResponse());

        // When
        paymentProcessService.processPaymentStatus(payload);

        // Then
        verify(odooApiClient, times(1)).sendPayment(anyString(), any());
        verify(alertService, never()).sendOdooApiFailureAlert(anyString(), anyString());
    }

    @Test
    @org.junit.jupiter.api.Order(8)
    @DisplayName("Should send alert on each Odoo API retry")
    void testSendToOdooWithRetry_AlertOnEachFailure() throws Exception {
        // Given
        when(tokenService.generateAccessToken(any(Order.class))).thenReturn("test.jwt.token");
        when(tokenService.generateDownloadUrl(anyString())).thenReturn("http://test.com");

        // Fail twice, then succeed
        when(odooApiClient.sendPayment(anyString(), any()))
                .thenThrow(new RuntimeException("Error 1"))
                .thenThrow(new RuntimeException("Error 2"))
                .thenReturn(mockOdooResponse());

        // When
        paymentProcessService.processPaymentStatus(payload);

        // Then
        verify(odooApiClient, times(3)).sendPayment(anyString(), any());
        verify(alertService, times(2)).sendOdooApiFailureAlert(eq("sendPaymentToOdoo"), anyString());
    }

    // ============= ERROR HANDLING & ALERTS =============

    @Test
    @org.junit.jupiter.api.Order(9)
    @DisplayName("Should send alert when employer reference not found")
    void testEnrichOrder_EmployerReferenceNotFound() {

        final Order order = new Order();
        order.setId(UUID.randomUUID());
        when(orderService.findOrCreateFromWebhook(payload, Order.OrderStatus.PENDING)).
                thenReturn(order);
        // Given
        payload.getReference().setReferenceNumber("INVALID-REF-999");

        // When & Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> paymentProcessService.processPaymentStatus(payload)
        );

        assertTrue(exception.getMessage().contains("Failed to process payment webhook"));
        verify(alertService, times(1)).sendEmployerReferenceNotFoundAlert(
                eq("INVALID-REF-999"),
                eq(TEST_TRANSACTION_ID)
        );
    }

    @Test
    @org.junit.jupiter.api.Order(10)
    @DisplayName("Should send alert when token generation fails")
    void testTokenGeneration_Failure() throws Exception {
        // Given
        when(tokenService.generateAccessToken(any(Order.class)))
                .thenThrow(new RuntimeException("JWT signing failed"));
        when(odooApiClient.sendPayment(anyString(), any())).thenReturn(mockOdooResponse());

        // When
        paymentProcessService.processPaymentStatus(payload);

        // Then
        verify(alertService, times(1)).sendTokenGenerationAlert(
                any(UUID.class),
                contains("JWT signing failed")
        );
        // Payment should still complete even if token generation fails
        verify(orderService, times(1)).transact(argThat(order ->
                order.getStatus() == Order.OrderStatus.COMPLETED
        ));
    }

    @Test
    @org.junit.jupiter.api.Order(11)
    @DisplayName("Should not generate token when employer info is missing")
    void testTokenGeneration_SkippedWhenEmployerMissing() throws Exception {
        // Given
        payload.setReference(null); // No reference info
        when(odooApiClient.sendPayment(anyString(), any())).thenReturn(mockOdooResponse());

        // When - Should not throw exception, just skip enrichment
        paymentProcessService.processPaymentStatus(payload);

        // Then - Token should not be generated
        verify(tokenService, never()).generateAccessToken(any(Order.class));
        verify(notificationService, never()).sendDocumentAccessEmail(anyString(), anyString(), any(), anyInt());
    }

    @Test
    @org.junit.jupiter.api.Order(12)
    @DisplayName("Should handle null reference info gracefully")
    void testProcessPayment_NullReferenceInfo() throws Exception {
        // Given
        payload.setReference(null);
        when(odooApiClient.sendPayment(anyString(), any())).thenReturn(mockOdooResponse());

        // When - Should not throw exception
        paymentProcessService.processPaymentStatus(payload);

        // Then - Should process payment but skip token generation
        verify(odooApiClient, times(1)).sendPayment(anyString(), any());
        verify(tokenService, never()).generateAccessToken(any(Order.class));
    }

    // ============= ENRICHMENT LOGIC TESTS =============

    @Test
    @org.junit.jupiter.api.Order(13)
    @DisplayName("Should extract reference code from payload")
    void testExtractReferenceCode() throws Exception {
        // Given
        when(tokenService.generateAccessToken(any(Order.class))).thenReturn("test.jwt.token");
        when(tokenService.generateDownloadUrl(anyString())).thenReturn("http://test.com");
        when(odooApiClient.sendPayment(anyString(), any())).thenReturn(mockOdooResponse());

        // When
        paymentProcessService.processPaymentStatus(payload);

        // Then
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderService).transact(orderCaptor.capture());

        Order enrichedOrder = orderCaptor.getValue();
        assertEquals(TEST_REFERENCE_CODE, enrichedOrder.getReferenceCode());
    }

    @Test
    @org.junit.jupiter.api.Order(14)
    @DisplayName("Should set package type (defaults to STANDARD)")
    void testDeterminePackageType() throws Exception {
        // Given
        when(tokenService.generateAccessToken(any(Order.class))).thenReturn("test.jwt.token");
        when(tokenService.generateDownloadUrl(anyString())).thenReturn("http://test.com");
        when(odooApiClient.sendPayment(anyString(), any())).thenReturn(mockOdooResponse());

        // When
        paymentProcessService.processPaymentStatus(payload);

        // Then
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderService).transact(orderCaptor.capture());

        Order enrichedOrder = orderCaptor.getValue();
        // Phase 1 implementation defaults to STANDARD
        assertEquals(PackageType.STANDARD, enrichedOrder.getPackageType());
    }

    @Test
    @org.junit.jupiter.api.Order(15)
    @DisplayName("Should extract candidate IDs (placeholder in Phase 1)")
    void testExtractCandidateIds() throws Exception {
        // Given
        when(tokenService.generateAccessToken(any(Order.class))).thenReturn("test.jwt.token");
        when(tokenService.generateDownloadUrl(anyString())).thenReturn("http://test.com");
        when(odooApiClient.sendPayment(anyString(), any())).thenReturn(mockOdooResponse());

        // When
        paymentProcessService.processPaymentStatus(payload);

        // Then
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderService).transact(orderCaptor.capture());

        Order enrichedOrder = orderCaptor.getValue();
        assertNotNull(enrichedOrder.getCandidateIds());
        assertFalse(enrichedOrder.getCandidateIds().isEmpty());
        // Phase 1 implementation returns placeholder: ["CAND-001", "CAND-002"]
        assertTrue(enrichedOrder.getCandidateIds().contains("CAND-001"));
    }

    @Test
    @org.junit.jupiter.api.Order(16)
    @DisplayName("Should map candidates to Odoo document IDs")
    void testMapCandidatesToOdooDocuments() throws Exception {
        // Given
        when(tokenService.generateAccessToken(any(Order.class))).thenReturn("test.jwt.token");
        when(tokenService.generateDownloadUrl(anyString())).thenReturn("http://test.com");
        when(odooApiClient.sendPayment(anyString(), any())).thenReturn(mockOdooResponse());

        // When
        paymentProcessService.processPaymentStatus(payload);

        // Then
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderService).transact(orderCaptor.capture());

        Order enrichedOrder = orderCaptor.getValue();
        assertNotNull(enrichedOrder.getOdooDocumentIds());
        assertFalse(enrichedOrder.getOdooDocumentIds().isEmpty());
        // Phase 1 implementation: simple mapping "odoo_doc_" + candidateId
        assertTrue(enrichedOrder.getOdooDocumentIds().get(0).startsWith("odoo_doc_"));
    }

    // ============= NOTIFICATION TESTS =============

    @Test
    @org.junit.jupiter.api.Order(17)
    @DisplayName("Should send email notification on successful payment")
    void testEmailNotification_Success() throws Exception {
        // Given
        when(tokenService.generateAccessToken(any(Order.class))).thenReturn("test.jwt.token");
        when(tokenService.generateDownloadUrl(anyString())).thenReturn("http://test.com/download?token=test");
        when(odooApiClient.sendPayment(anyString(), any())).thenReturn(mockOdooResponse());

        // When
        paymentProcessService.processPaymentStatus(payload);

        // Then
        verify(notificationService, times(1)).sendDocumentAccessEmail(
                eq("employer@test.com"),
                eq("http://test.com/download?token=test"),
                eq(PackageType.STANDARD),
                eq(2) // Placeholder returns 2 candidates
        );
    }

    @Test
    @org.junit.jupiter.api.Order(18)
    @DisplayName("Should not send email for non-SUCCESS statuses")
    void testEmailNotification_OnlyForSuccess() throws Exception {
        // Given
        payload.setStatus("PENDING");
        when(odooApiClient.sendPayment(anyString(), any())).thenReturn(mockOdooResponse());

        // When
        paymentProcessService.processPaymentStatus(payload);

        // Then
        verify(notificationService, never()).sendDocumentAccessEmail(
                anyString(), anyString(), any(), anyInt()
        );
    }

    // ============= EDGE CASES =============

    @Test
    @org.junit.jupiter.api.Order(19)
    @DisplayName("Should handle unknown payment status gracefully")
    void testProcessPayment_UnknownStatus() throws Exception {
        // Given
        payload.setStatus("UNKNOWN_STATUS");

        // When
        paymentProcessService.processPaymentStatus(payload);

        // Then - should not crash, just log warning
        verify(orderService, never()).transact(any(Order.class));
    }

    @Test
    @org.junit.jupiter.api.Order(20)
    @DisplayName("Should create payment transaction for all statuses")
    void testPaymentTransaction_CreatedForAllStatuses() throws Exception {
        // Given
        when(odooApiClient.sendPayment(anyString(), any())).thenReturn(mockOdooResponse());

        // When - Test multiple statuses
        payload.setStatus("SUCCESS");
        paymentProcessService.processPaymentStatus(payload);

        payload.setStatus("FAILED");
        paymentProcessService.processPaymentStatus(payload);

        payload.setStatus("PENDING");
        paymentProcessService.processPaymentStatus(payload);

        // Then — SUCCESS and PENDING call Odoo; FAILED does NOT (bug fixed in PaymentProcessService)
        verify(odooApiClient, times(2)).sendPayment(anyString(), any());
    }
}