package ao.co.oportunidade.webhook.service;

import ao.co.oportunidade.odoo.service.OdooPaymentService;
import ao.co.oportunidade.service.BasicApplicationService;
import ao.co.oportunidade.webhook.*;
import ao.co.oportunidade.webhook.dto.AppyPayWebhookPayload;
import ao.co.oportunidade.webhook.dto.ReferenceInfo;
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
public class ProcessPaymentService extends
        BasicApplicationService<PaymentTransaction, PaymentTransactionRepository,
                Order,OrderRepository,PaymentTransactionService,OrderService> {

    private static final Logger LOG = Logger.getLogger(ProcessPaymentService.class);

    private OdooPaymentService odooPaymentService;


    public ProcessPaymentService(OdooPaymentService odooPaymentService) {
        this.odooPaymentService = odooPaymentService;
    }

    /**
     * Process the AppyPay webhook payload.
     * Routes to appropriate handler based on payment status.
     *
     * @param payload the webhook payload
     */
    @Transactional
    public void processWebhook(final AppyPayWebhookPayload payload) throws RuntimeException {
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

    private void handleSuccessfulPayment(final AppyPayWebhookPayload payload) {
        LOG.infof("Handling successful payment: %s", payload.getId());

        final OrderService orderService = getSupportingDomainService();
        final Order order = orderService.findOrCreateOrder(payload, Order.OrderStatus.PAID);
        order.setStatus(Order.OrderStatus.PAID);
        orderService.updateOrder(order);

        final PaymentTransaction paymentTransaction = createPaymentTransaction(payload, order, PaymentTransaction.TransactionStatus.SUCCESS);
        odooPaymentService.sendPaymentToOdoo(paymentTransaction);

        LOG.infof("Successfully processed payment for order: %s",
                order.getMerchantTransactionId());
    }

    private void handlePendingPayment(final AppyPayWebhookPayload payload) {
        LOG.infof("Handling pending payment: %s", payload.getId());

        final Order order = getSupportingDomainService().findOrCreateOrder(payload, Order.OrderStatus.PENDING);
        getSupportingDomainService().createDomain(order);

        createPaymentTransaction(payload, order, PaymentTransaction.TransactionStatus.PENDING);

        LOG.infof("Payment pending for order: %s", order.getMerchantTransactionId());
    }

    private void handleFailedPayment(final AppyPayWebhookPayload payload) {
        LOG.infof("Handling failed payment: %s", payload.getId());

        final Order order = getSupportingDomainService().findOrCreateOrder(payload, Order.OrderStatus.FAILED);
        order.setStatus(Order.OrderStatus.FAILED);
        getSupportingDomainService().updateOrder(order);

        final PaymentTransaction transaction = createPaymentTransaction(
                payload, order, PaymentTransaction.TransactionStatus.FAILED);
        
        if (payload.getResponseStatus() != null) {
            transaction.setErrorMessage(payload.getResponseStatus().getMessage());
            getMainDomainService().createDomain(transaction);
        }

        LOG.infof("Payment failed for order: %s", order.getMerchantTransactionId());
    }

    private void handleCancelledPayment(final AppyPayWebhookPayload payload) {
        LOG.infof("Handling cancelled payment: %s", payload.getId());

        final Optional<Order> existingOrder = getSupportingDomainService()
                .findByMerchantTransactionId(payload.getMerchantTransactionId());

        if (existingOrder.isPresent()) {
            final                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      Order order = existingOrder.get();
            order.setStatus(Order.OrderStatus.CANCELLED);
            getSupportingDomainService().updateOrder(order);

            createPaymentTransaction(payload, order, PaymentTransaction.TransactionStatus.CANCELLED);

            LOG.infof("Payment cancelled for order: %s", order.getMerchantTransactionId());
        } else {
            LOG.warnf("Order not found for cancelled payment: %s", payload.getMerchantTransactionId());
        }
    }

    private PaymentTransaction createPaymentTransaction(
            final AppyPayWebhookPayload payload,
            final Order order,
            final PaymentTransaction.TransactionStatus status) {

        final PaymentTransaction transaction = new PaymentTransaction();
        transaction.setId(UUID.randomUUID());
        transaction.setOrderId(order.getId());
        transaction.setAppypayTransactionId(payload.getId());
        transaction.setAmount(payload.getAmount());
        transaction.setCurrency(payload.getCurrency());
        transaction.setStatus(status);
        transaction.setPaymentMethod(payload.getPaymentMethod());
        transaction.setTransactionDate(payload.getCreatedDate() != null ?
                payload.getCreatedDate() : Instant.now());
        transaction.setCreatedDate(Instant.now());
        transaction.setUpdatedDate(Instant.now());

        // Set reference info if available
        if (payload.getReference() != null) {
            ReferenceInfo refInfo = payload.getReference();
            transaction.setReferenceNumber(refInfo.getReferenceNumber());
            transaction.setReferenceEntity(refInfo.getEntity());
        }

        getMainDomainService().createDomain(transaction);
        LOG.infof("Created payment transaction: %s for order: %s",
                transaction.getId(), order.getId());

        return transaction;
    }
}
