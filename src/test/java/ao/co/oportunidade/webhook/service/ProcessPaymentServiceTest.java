package ao.co.oportunidade.webhook.service;

import ao.co.oportunidade.Reference;
import ao.co.oportunidade.ReferenceRepository;
import ao.co.oportunidade.webhook.Order;
import ao.co.oportunidade.webhook.OrderRepository;
import ao.co.oportunidade.webhook.PaymentTransaction;
import ao.co.oportunidade.webhook.PaymentTransactionRepository;
import ao.co.oportunidade.webhook.dto.AppyPayWebhookPayload;
import ao.co.oportunidade.webhook.dto.CustomerInfo;
import ao.co.oportunidade.webhook.dto.ReferenceInfo;
import ao.co.oportunidade.webhook.entity.*;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentService.
 */
//@QuarkusTest
class ProcessPaymentServiceTest {

    private final static UUID ORDER_ID = UUID.randomUUID();
    public static final String BASIC_REFERENCE_NO = "123456789";
    public static final int WANTED_NUMBER_OF_INVOCATIONS = 1;

    @Inject
    ProcessPaymentService processPaymentService;

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


        // Reset all mocks after each test
        final ReferenceInfo refInfo = ReferenceInfo.builder()
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


        final OrderEntity orderEntity = new OrderEntity();
        orderEntity.setId(ORDER_ID);
        orderEntity.setAmount(new BigDecimal("1500.00"));
        orderEntity.setMerchantTransactionId("ORDER-12345");
        orderEntity.setStatus("Pending");


        verify(orderEntity, times(WANTED_NUMBER_OF_INVOCATIONS)).persist();

        final Reference reference = new Reference();
        reference.setReferenceNumber(BASIC_REFERENCE_NO);
        when(referenceRepository.findByReferenceNumber(BASIC_REFERENCE_NO))
                .thenReturn(Optional.of(reference));
    }


    @AfterEach
    void tearDown() {
        // Reset all mocks after each test
        reset(orderRepository, paymentTransactionRepository, referenceRepository);
    }

    @Test
    void testProcessWebhook_Success_CreatesOrderAndTransaction() {
        // When
        processPaymentService.processWebhook(successPayload);

        // Then - verify order was created/updated
        final ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository, atLeastOnce()).persist(orderCaptor.capture());
        
        final OrderEntity capturedOrder = orderCaptor.getValue();
        assertThat(capturedOrder.getMerchantTransactionId()               ).isEqualTo("ORDER-12345");
        assertThat(capturedOrder.getAmount()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(capturedOrder.getCurrency()).isEqualTo("AOA");
        assertThat(capturedOrder.getStatus().toLowerCase()).isEqualTo(Order.OrderStatus.PAID.toString().toLowerCase());
        assertThat(capturedOrder.getCustomerName()).isEqualTo("John Doe");
        assertThat(capturedOrder.getCustomerEmail()).isEqualTo("john@example.com");

        // Then - verify transaction was created
        final ArgumentCaptor<PaymentTransactionEntity> txCaptor = ArgumentCaptor.forClass(PaymentTransactionEntity.class);
        verify(paymentTransactionRepository).persist(txCaptor.capture());
        
        final PaymentTransactionEntity capturedTx = txCaptor.getValue();
        assertThat(capturedTx.getAppypayTransactionId()).isEqualTo("tx-success-123");
        assertThat(capturedTx.getStatus().toLowerCase()).isEqualTo(PaymentTransaction.TransactionStatus.SUCCESS.toString().toLowerCase());
        assertThat(capturedTx.getReferenceNumber()).isEqualTo("123456789");
    }

    @Test
    void testProcessWebhook_Pending_CreatesOrderWithPendingStatus() {
        // When
        processPaymentService.processWebhook(pendingPayload);

        // Then
        final ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository, atLeastOnce()).persist(orderCaptor.capture());
        
        final OrderEntity capturedOrder = orderCaptor.getValue();
        assertThat(capturedOrder.getStatus()).isEqualTo(Order.OrderStatus.PENDING.toString().toLowerCase());

        // Then - verify transaction was created with pending status
        final ArgumentCaptor<PaymentTransactionEntity> txCaptor = ArgumentCaptor.forClass(PaymentTransactionEntity.class);
        verify(paymentTransactionRepository).persist(txCaptor.capture());
        
        final PaymentTransactionEntity capturedTx = txCaptor.getValue();
        assertThat(capturedTx.getStatus().toLowerCase()).isEqualTo(PaymentTransaction.TransactionStatus.PENDING.toString().toLowerCase());
    }

    @Test
    void testProcessWebhook_Failed_CreatesOrderWithFailedStatus() {
        // When
        processPaymentService.processWebhook(failedPayload);

        // Then
        final ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository, atLeastOnce()).persist(orderCaptor.capture());
        
        final OrderEntity capturedOrder = orderCaptor.getValue();
        assertThat(capturedOrder.getStatus().toLowerCase()).isEqualTo(Order.OrderStatus.FAILED.toString().toLowerCase());

        // Then - verify transaction was created with failed status
        final ArgumentCaptor<PaymentTransactionEntity> txCaptor = ArgumentCaptor.forClass(PaymentTransactionEntity.class);
        verify(paymentTransactionRepository).persist(txCaptor.capture());
        
        final PaymentTransactionEntity capturedTx = txCaptor.getValue();
        assertThat(capturedTx.getStatus().toLowerCase()).isEqualTo(PaymentTransaction.TransactionStatus.FAILED.toString().toLowerCase());
    }

    @Test
    void testProcessWebhook_UpdatesExistingOrder() {
        // Given - existing order
        final Order existingOrder = Order.builder()
                .id(ORDER_ID)
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
        processPaymentService.processWebhook(successPayload);

        // Then - verify order was updated to PAID
        assertThat(existingOrder.getStatus())
                .isEqualTo(Order.OrderStatus.PAID);
        verify(orderRepository).createDomain(existingOrder);
    }

    @Test
    void testProcessWebhook_LinksToExistingReference() {
        // Given - existing reference
        final Reference existingRef = new Reference();
        existingRef.setReferenceNumber("123456789");
        
        when(referenceRepository.findByReferenceNumber("123456789"))
                .thenReturn(Optional.of(existingRef));

        // When
        processPaymentService.processWebhook(successPayload);

        // Then - verify order was linked to reference
        final ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository, atLeastOnce()).persist(orderCaptor.capture());
        
        final OrderEntity capturedOrder = orderCaptor.getValue();
        assertThat(capturedOrder.getReferenceId()).isEqualTo(existingRef.getId());
    }

    @Test
    void testProcessWebhook_Cancelled() {
        // Given - existing order
        final Order existingOrder = Order.builder()
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

        final AppyPayWebhookPayload cancelledPayload = AppyPayWebhookPayload.builder()
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
        processPaymentService.processWebhook(cancelledPayload);

        // Then
        assertThat(existingOrder.getStatus()).isEqualTo(Order.OrderStatus.CANCELLED);
        
        final ArgumentCaptor<PaymentTransactionEntity> txCaptor = ArgumentCaptor.forClass(PaymentTransactionEntity.class);
        verify(paymentTransactionRepository).persist(txCaptor.capture());
        
        final PaymentTransactionEntity capturedTx = txCaptor.getValue();
        assertThat(capturedTx.getStatus().toLowerCase()).
                isEqualTo(PaymentTransaction.TransactionStatus.CANCELLED.toString().toLowerCase());
    }
}
