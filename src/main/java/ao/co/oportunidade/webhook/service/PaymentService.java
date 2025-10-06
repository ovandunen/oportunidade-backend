package ao.co.oportunidade.webhook.service;

import ao.co.oportunidade.ReferenceRepository;
import ao.co.oportunidade.webhook.dto.AppyPayWebhookPayload;
import ao.co.oportunidade.webhook.dto.CustomerInfo;
import ao.co.oportunidade.webhook.dto.ReferenceInfo;
import ao.co.oportunidade.webhook.entity.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for processing payment webhooks and managing payment lifecycle.
 */
@ApplicationScoped
public class PaymentService {

    private static final Logger LOG = Logger.getLogger(PaymentService.class);

    @Inject
    OrderRepository orderRepository;

    @Inject
    PaymentTransactionRepository paymentTransactionRepository;

    @Inject
    ReferenceRepository referenceRepository;

    /**
     * Process the AppyPay webhook payload.
     * Routes to appropriate handler based on payment status.
     *
     * @param payload the webhook payload
     */
    @Transactional
    public void processWebhook(AppyPayWebhookPayload payload) {
        LOG.infof("Processing webhook for transaction: %s, status: %s", 
                payload.getId(), payload.getStatus());

        try {
            switch (payload.getStatus().toUpperCase()) {
                case "SUCCESS":
                    handleSuccessfulPayment(payload);
                    break;
                case "PENDING":
                    handlePendingPayment(payload);
                    break;
                case "FAILED":
                    handleFailedPayment(payload);
                    break;
                case "CANCELLED":
                    handleCancelledPayment(payload);
                    break;
                default:
                    LOG.warnf("Unknown payment status: %s for transaction: %s", 
                            payload.getStatus(), payload.getId());
            }
        } catch (Exception e) {
            LOG.errorf(e, "Error processing webhook for transaction: %s", payload.getId());
            throw new RuntimeException("Failed to process payment webhook", e);
        }
    }

    /**
     * Handle successful payment.
     */
    private void handleSuccessfulPayment(AppyPayWebhookPayload payload) {
        LOG.infof("Handling successful payment: %s", payload.getId());

        // Find or create order
        Order order = findOrCreateOrder(payload, Order.OrderStatus.PAID);
        order.setStatus(Order.OrderStatus.PAID);
        orderRepository.persist(order);

        // Create payment transaction
        createPaymentTransaction(payload, order, PaymentTransaction.TransactionStatus.SUCCESS);

        LOG.infof("Successfully processed payment for order: %s", order.getMerchantTransactionId());
    }

    /**
     * Handle pending payment.
     */
    private void handlePendingPayment(AppyPayWebhookPayload payload) {
        LOG.infof("Handling pending payment: %s", payload.getId());

        // Find or create order with pending status
        Order order = findOrCreateOrder(payload, Order.OrderStatus.PENDING);
        orderRepository.persist(order);

        // Create payment transaction
        createPaymentTransaction(payload, order, PaymentTransaction.TransactionStatus.PENDING);

        LOG.infof("Payment pending for order: %s", order.getMerchantTransactionId());
    }

    /**
     * Handle failed payment.
     */
    private void handleFailedPayment(AppyPayWebhookPayload payload) {
        LOG.infof("Handling failed payment: %s", payload.getId());

        // Find or create order
        Order order = findOrCreateOrder(payload, Order.OrderStatus.FAILED);
        order.setStatus(Order.OrderStatus.FAILED);
        orderRepository.persist(order);

        // Create payment transaction with error info
        PaymentTransaction transaction = createPaymentTransaction(
                payload, order, PaymentTransaction.TransactionStatus.FAILED);
        
        if (payload.getResponseStatus() != null) {
            transaction.setErrorMessage(payload.getResponseStatus().getMessage());
            paymentTransactionRepository.persist(transaction);
        }

        LOG.infof("Payment failed for order: %s", order.getMerchantTransactionId());
    }

    /**
     * Handle cancelled payment.
     */
    private void handleCancelledPayment(AppyPayWebhookPayload payload) {
        LOG.infof("Handling cancelled payment: %s", payload.getId());

        // Find existing order
        Optional<Order> existingOrder = orderRepository
                .findByMerchantTransactionId(payload.getMerchantTransactionId());

        if (existingOrder.isPresent()) {
            Order order = existingOrder.get();
            order.setStatus(Order.OrderStatus.CANCELLED);
            orderRepository.persist(order);

            // Create payment transaction
            createPaymentTransaction(payload, order, PaymentTransaction.TransactionStatus.CANCELLED);

            LOG.infof("Payment cancelled for order: %s", order.getMerchantTransactionId());
        } else {
            LOG.warnf("Order not found for cancelled payment: %s", payload.getMerchantTransactionId());
        }
    }

    /**
     * Find existing order or create a new one.
     */
    private Order findOrCreateOrder(AppyPayWebhookPayload payload, Order.OrderStatus defaultStatus) {
        Optional<Order> existingOrder = orderRepository
                .findByMerchantTransactionId(payload.getMerchantTransactionId());

        if (existingOrder.isPresent()) {
            return existingOrder.get();
        }

        // Create new order
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .merchantTransactionId(payload.getMerchantTransactionId())
                .amount(payload.getAmount())
                .currency(payload.getCurrency())
                .status(defaultStatus)
                .createdDate(Instant.now())
                .updatedDate(Instant.now())
                .build();

        // Set customer info if available
        if (payload.getCustomer() != null) {
            CustomerInfo customer = payload.getCustomer();
            order.setCustomerName(customer.getName());
            order.setCustomerEmail(customer.getEmail());
            order.setCustomerPhone(customer.getPhone());
        }

        // Link to reference if available
        if (payload.getReference() != null) {
            ReferenceInfo refInfo = payload.getReference();
            // Try to find existing reference by reference number
            referenceRepository.findByReferenceNumber(refInfo.getReferenceNumber())
                    .ifPresent(ref -> order.setReferenceId(ref.getId()));
        }

        return order;
    }

    /**
     * Create a payment transaction record.
     */
    private PaymentTransaction createPaymentTransaction(
            AppyPayWebhookPayload payload,
            Order order,
            PaymentTransaction.TransactionStatus status) {

        PaymentTransaction transaction = PaymentTransaction.builder()
                .id(UUID.randomUUID())
                .orderId(order.getId())
                .appypayTransactionId(payload.getId())
                .amount(payload.getAmount())
                .currency(payload.getCurrency())
                .status(status)
                .paymentMethod(payload.getPaymentMethod())
                .transactionDate(payload.getCreatedDate() != null ? 
                        payload.getCreatedDate() : Instant.now())
                .createdDate(Instant.now())
                .updatedDate(Instant.now())
                .build();

        // Set reference info if available
        if (payload.getReference() != null) {
            ReferenceInfo refInfo = payload.getReference();
            transaction.setReferenceNumber(refInfo.getReferenceNumber());
            transaction.setReferenceEntity(refInfo.getEntity());
        }

        paymentTransactionRepository.persist(transaction);
        LOG.infof("Created payment transaction: %s for order: %s", 
                transaction.getId(), order.getId());

        return transaction;
    }
}
