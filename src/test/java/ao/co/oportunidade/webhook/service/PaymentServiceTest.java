package ao.co.oportunidade.webhook.service;

import ao.co.oportunidade.Reference;
import ao.co.oportunidade.ReferenceRepository;
import ao.co.oportunidade.webhook.dto.AppyPayWebhookPayload;
import ao.co.oportunidade.webhook.dto.CustomerInfo;
import ao.co.oportunidade.webhook.dto.ReferenceInfo;
import ao.co.oportunidade.webhook.entity.*;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentService.
 */
@QuarkusTest
class PaymentServiceTest {

    @Inject
    PaymentService paymentService;

    @InjectMock
    OrderRepository orderRepository;

    @InjectMock
    PaymentTransactionRepository paymentTransactionRepository;

    @InjectMock
    ReferenceRepository referenceRepository;

    private AppyPayWebhookPayload successPayload;
    private AppyPayWebhookPayload pendingPayload;
    private AppyPayWebhookPayload failedPayload;

    @BeforeEach
    void setUp() {
        ReferenceInfo refInfo = ReferenceInfo.builder()
                .referenceNumber("123456789")
                .entity("00123")
                .dueDate(Instant.now().plusSeconds(86400))
                .build();

        CustomerInfo customerInfo = CustomerInfo.builder()
                .name("John Doe")
                .email("john@example.com")
                .phone("+244900000000")
                .build();

        successPayload = AppyPayWebhookPayload.builder()
                .id("tx-success-123")
                .merchantTransactionId("ORDER-12345")
                .type("Charge")
                .amount(new BigDecimal("1500.00"))
                .currency("AOA")
                .status("Success")
                .paymentMethod("REF")
                .reference(refInfo)
                .customer(customerInfo)
                .createdDate(Instant.now())
                .updatedDate(Instant.now())
                .build();

        pendingPayload = AppyPayWebhookPayload.builder()
                .id("tx-pending-456")
                .merchantTransactionId("ORDER-67890")
                .type("Charge")
                .amount(new BigDecimal("2000.00"))
                .currency("AOA")
                .status("Pending")
                .paymentMethod("REF")
                .reference(refInfo)
                .customer(customerInfo)
                .createdDate(Instant.now())
                .updatedDate(Instant.now())
                .build();

        failedPayload = AppyPayWebhookPayload.builder()
                .id("tx-failed-789")
                .merchantTransactionId("ORDER-99999")
                .type("Charge")
                .amount(new BigDecimal("3000.00"))
                .currency("AOA")
                .status("Failed")
                .paymentMethod("REF")
                .reference(refInfo)
                .customer(customerInfo)
                .createdDate(Instant.now())
                .updatedDate(Instant.now())
                .build();

        // Setup common mocks
        when(orderRepository.findByMerchantTransactionId(anyString()))
                .thenReturn(Optional.empty());
        when(referenceRepository.findByReferenceNumber(anyString()))
                .thenReturn(Optional.empty());
    }

    @Test
    void testProcessWebhook_Success_CreatesOrderAndTransaction() {
        // When
        paymentService.processWebhook(successPayload);

        // Then - verify order was created/updated
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, atLeastOnce()).persist(orderCaptor.capture());
        
        Order capturedOrder = orderCaptor.getValue();
        assertThat(capturedOrder.getMerchantTransactionId()).isEqualTo("ORDER-12345");
        assertThat(capturedOrder.getAmount()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(capturedOrder.getCurrency()).isEqualTo("AOA");
        assertThat(capturedOrder.getStatus()).isEqualTo(Order.OrderStatus.PAID);
        assertThat(capturedOrder.getCustomerName()).isEqualTo("John Doe");
        assertThat(capturedOrder.getCustomerEmail()).isEqualTo("john@example.com");

        // Then - verify transaction was created
        ArgumentCaptor<PaymentTransaction> txCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository).persist(txCaptor.capture());
        
        PaymentTransaction capturedTx = txCaptor.getValue();
        assertThat(capturedTx.getAppypayTransactionId()).isEqualTo("tx-success-123");
        assertThat(capturedTx.getStatus()).isEqualTo(PaymentTransaction.TransactionStatus.SUCCESS);
        assertThat(capturedTx.getReferenceNumber()).isEqualTo("123456789");
    }

    @Test
    void testProcessWebhook_Pending_CreatesOrderWithPendingStatus() {
        // When
        paymentService.processWebhook(pendingPayload);

        // Then
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, atLeastOnce()).persist(orderCaptor.capture());
        
        Order capturedOrder = orderCaptor.getValue();
        assertThat(capturedOrder.getStatus()).isEqualTo(Order.OrderStatus.PENDING);

        // Then - verify transaction was created with pending status
        ArgumentCaptor<PaymentTransaction> txCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository).persist(txCaptor.capture());
        
        PaymentTransaction capturedTx = txCaptor.getValue();
        assertThat(capturedTx.getStatus()).isEqualTo(PaymentTransaction.TransactionStatus.PENDING);
    }

    @Test
    void testProcessWebhook_Failed_CreatesOrderWithFailedStatus() {
        // When
        paymentService.processWebhook(failedPayload);

        // Then
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, atLeastOnce()).persist(orderCaptor.capture());
        
        Order capturedOrder = orderCaptor.getValue();
        assertThat(capturedOrder.getStatus()).isEqualTo(Order.OrderStatus.FAILED);

        // Then - verify transaction was created with failed status
        ArgumentCaptor<PaymentTransaction> txCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository).persist(txCaptor.capture());
        
        PaymentTransaction capturedTx = txCaptor.getValue();
        assertThat(capturedTx.getStatus()).isEqualTo(PaymentTransaction.TransactionStatus.FAILED);
    }

    @Test
    void testProcessWebhook_UpdatesExistingOrder() {
        // Given - existing order
        Order existingOrder = Order.builder()
                .id(UUID.randomUUID())
                .merchantTransactionId("ORDER-12345")
                .amount(new BigDecimal("1500.00"))
                .currency("AOA")
                .status(Order.OrderStatus.PENDING)
                .createdDate(Instant.now())
                .updatedDate(Instant.now())
                .build();
        
        when(orderRepository.findByMerchantTransactionId("ORDER-12345"))
                .thenReturn(Optional.of(existingOrder));

        // When
        paymentService.processWebhook(successPayload);

        // Then - verify order was updated to PAID
        assertThat(existingOrder.getStatus()).isEqualTo(Order.OrderStatus.PAID);
        verify(orderRepository).persist(existingOrder);
    }

    @Test
    void testProcessWebhook_LinksToExistingReference() {
        // Given - existing reference
        Reference existingRef = new Reference();
        existingRef.setReferenceNumber("123456789");
        
        when(referenceRepository.findByReferenceNumber("123456789"))
                .thenReturn(Optional.of(existingRef));

        // When
        paymentService.processWebhook(successPayload);

        // Then - verify order was linked to reference
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, atLeastOnce()).persist(orderCaptor.capture());
        
        Order capturedOrder = orderCaptor.getValue();
        assertThat(capturedOrder.getReferenceId()).isEqualTo(existingRef.getId());
    }

    @Test
    void testProcessWebhook_Cancelled() {
        // Given - existing order
        Order existingOrder = Order.builder()
                .id(UUID.randomUUID())
                .merchantTransactionId("ORDER-12345")
                .amount(new BigDecimal("1500.00"))
                .currency("AOA")
                .status(Order.OrderStatus.PENDING)
                .createdDate(Instant.now())
                .updatedDate(Instant.now())
                .build();
        
        when(orderRepository.findByMerchantTransactionId("ORDER-12345"))
                .thenReturn(Optional.of(existingOrder));

        AppyPayWebhookPayload cancelledPayload = AppyPayWebhookPayload.builder()
                .id("tx-cancelled-123")
                .merchantTransactionId("ORDER-12345")
                .type("Charge")
                .amount(new BigDecimal("1500.00"))
                .currency("AOA")
                .status("Cancelled")
                .paymentMethod("REF")
                .createdDate(Instant.now())
                .updatedDate(Instant.now())
                .build();

        // When
        paymentService.processWebhook(cancelledPayload);

        // Then
        assertThat(existingOrder.getStatus()).isEqualTo(Order.OrderStatus.CANCELLED);
        
        ArgumentCaptor<PaymentTransaction> txCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository).persist(txCaptor.capture());
        
        PaymentTransaction capturedTx = txCaptor.getValue();
        assertThat(capturedTx.getStatus()).isEqualTo(PaymentTransaction.TransactionStatus.CANCELLED);
    }
}
