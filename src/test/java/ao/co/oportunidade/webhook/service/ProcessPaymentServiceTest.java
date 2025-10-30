package ao.co.oportunidade.webhook.service;

import ao.co.oportunidade.Reference;
import ao.co.oportunidade.ReferenceRepository;
import ao.co.oportunidade.webhook.*;
import ao.co.oportunidade.webhook.Order;
import ao.co.oportunidade.webhook.dto.AppyPayWebhookPayload;
import ao.co.oportunidade.webhook.dto.CustomerInfo;
import ao.co.oportunidade.webhook.dto.ReferenceInfo;
import ao.co.oportunidade.webhook.entity.*;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentService.
 */

//@Disabled("Temporarily ignoring this test")
@QuarkusTest
class ProcessPaymentServiceTest {

    private static final UUID ORDER_ID = UUID.randomUUID();
    public static final String BASIC_REFERENCE_NO = "123456789";
    public static final int WANTED_NUMBER_OF_INVOCATIONS = 1;
    public static final String ANGOLAN_CURRENCY = "AOA";
    public static final UUID REFERENCE_ID = UUID.randomUUID();
    private static final String REFERENCE_NUMBER = "123456789";

    private Reference reference;

    @Inject
    ProcessPaymentService processPaymentService;

    @InjectMock
    OrderRepository orderRepository;

    @InjectMock
    OrderService orderService;

    @InjectMock
    PaymentTransactionService paymentTransactionService;

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

        final CustomerInfo customerInfo = CustomerInfo.builder()
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

        reference = new Reference();
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

        final Order order = new Order();
        order.setId(ORDER_ID);
        order.setAmount(new BigDecimal("1500.00"));
        order.setMerchantTransactionId("ORDER-12345");
        order.setStatus(Order.OrderStatus.PAID);
        order.setCurrency(Currency.getInstance(ANGOLAN_CURRENCY).getCurrencyCode());
        order.setCreatedDate(Instant.now());
        order.setUpdatedDate(Instant.now());
        order.setCustomerName("John Doe");
        order.setCustomerEmail("john@example.com");
        order.setReferenceId(reference.getId());

        when(orderService.find(successPayload)).thenReturn(order);
        when(orderService.findByMerchantTransactionId(anyString())).thenReturn(Optional.of(order));
        when(orderRepository.findDomainById(order))
                .thenReturn(Optional.of(order));
        when(orderService.find(pendingPayload)).thenReturn(order);



        // When
        processPaymentService.processWebhook(successPayload);

        // Then - verify order was created/updated
        final ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderService, atLeastOnce()).saveDomain(orderCaptor.capture());

        final Order capturedOrder = orderCaptor.getValue();
        assertThat(capturedOrder.getMerchantTransactionId()).isEqualTo("ORDER-12345");
        assertThat(capturedOrder.getAmount()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(capturedOrder.getCurrency()).isEqualTo("AOA");
        assertThat(capturedOrder.getStatus()).isEqualTo(Order.OrderStatus.PAID);
        assertThat(capturedOrder.getCustomerName()).isEqualTo("John Doe");
        assertThat(capturedOrder.getCustomerEmail()).isEqualTo("john@example.com");

        // Then - verify transaction was created
        final ArgumentCaptor<PaymentTransaction> txCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionService).saveDomain(txCaptor.capture());
        
        final PaymentTransaction capturedTx = txCaptor.getValue();
        assertThat(capturedTx.getAppypayTransactionId()).isEqualTo("tx-success-123");
        assertThat(capturedTx.getStatus()).isEqualTo(PaymentTransaction.TransactionStatus.SUCCESS);
        assertThat(capturedTx.getReferenceNumber()).isEqualTo("123456789");
    }

    @Test
    void testProcessWebhook_Pending_CreatesOrderWithPendingStatus() {

        final Order order = new Order();
        order.setId(ORDER_ID);
        order.setAmount(new BigDecimal("1500.00"));
        order.setMerchantTransactionId("ORDER-12345");
        order.setStatus(Order.OrderStatus.PENDING);
        order.setCurrency(Currency.getInstance(ANGOLAN_CURRENCY).getCurrencyCode());
        order.setCreatedDate(Instant.now());
        order.setUpdatedDate(Instant.now());
        order.setCustomerName("John Doe");
        order.setCustomerEmail("john@example.com");
        order.setReferenceId(reference.getId());

        when(orderService.find(pendingPayload)).thenReturn(order);
        when(orderService.findByMerchantTransactionId(anyString())).thenReturn(Optional.of(order));
        when(orderRepository.findDomainById(order))
                .thenReturn(Optional.of(order));

        // When
        processPaymentService.processWebhook(pendingPayload);

        // Then
        final ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderService, atLeastOnce()).saveDomain(orderCaptor.capture());

        assertThat(orderCaptor.getValue().getStatus()).isEqualTo(Order.OrderStatus.PENDING);

    }

    @Test
    void testProcessWebhook_Failed_CreatesOrderWithFailedStatus() {

        final Order order = new Order();
        order.setId(ORDER_ID);
        order.setAmount(new BigDecimal("1500.00"));
        order.setMerchantTransactionId("ORDER-12345");
        order.setStatus(Order.OrderStatus.FAILED);
        order.setCurrency(Currency.getInstance(ANGOLAN_CURRENCY).getCurrencyCode());
        order.setCreatedDate(Instant.now());
        order.setUpdatedDate(Instant.now());
        order.setCustomerName("John Doe");
        order.setCustomerEmail("john@example.com");
        order.setReferenceId(reference.getId());

        when(orderService.find(failedPayload)).thenReturn(order);
        when(orderService.findByMerchantTransactionId(anyString())).thenReturn(Optional.of(order));
        when(orderRepository.findDomainById(order))
                .thenReturn(Optional.of(order));


        // When
        processPaymentService.processWebhook(failedPayload);

        final ArgumentCaptor<Order> txCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderService).saveDomain(txCaptor.capture());
        assertThat(txCaptor.getValue().getStatus()).isEqualTo(Order.OrderStatus.FAILED);

        // Then - verify transaction was created with failed status
        final ArgumentCaptor<PaymentTransaction> transactionArgumentCaptor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionService).saveDomain(transactionArgumentCaptor.capture());
        assertThat(transactionArgumentCaptor.getValue().getStatus()).isEqualTo(PaymentTransaction.TransactionStatus.FAILED);
    }

    @Test
    void testProcessWebhook_LinksToExistingReference() {

        final Order order = new Order();
        order.setId(ORDER_ID);
        order.setAmount(new BigDecimal("1500.00"));
        order.setMerchantTransactionId("ORDER-12345");
        order.setStatus(Order.OrderStatus.PAID);
        order.setCurrency(Currency.getInstance(ANGOLAN_CURRENCY).getCurrencyCode());
        order.setCreatedDate(Instant.now());
        order.setUpdatedDate(Instant.now());
        order.setCustomerName("John Doe");
        order.setCustomerEmail("john@example.com");
        order.setReferenceId(reference.getId());

        when(orderService.find(successPayload)).thenReturn(order);
        processPaymentService.processWebhook(successPayload);

        // Then - verify order was linked to reference
        final ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderService, atLeastOnce()).saveDomain(orderCaptor.capture());

        final Order capturedOrder = orderCaptor.getValue();
        assertThat(capturedOrder.getReferenceId()).isEqualTo(reference.getId());
    }

}
