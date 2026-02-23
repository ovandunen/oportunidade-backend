package ao.co.oportunidade.payment.service;

import ao.co.oportunidade.order.entity.OrderRepository;
import ao.co.oportunidade.order.model.Order;
import ao.co.oportunidade.order.service.OrderService;
import ao.co.oportunidade.payment.entity.PaymentTransactionRepository;
import ao.co.oportunidade.payment.model.PaymentTransaction;
import solutions.envision.odoo.service.OdooPaymentService;
import solutions.envision.service.BasicApplicationService;
import solutions.envision.odoo.document.service.DocumentTokenService;
import ao.co.oportunidade.webhook.dto.AppyPayWebhookPayload;
import ao.co.oportunidade.webhook.dto.ReferenceInfo;
import ao.co.oportunidade.employer.model.EmployerReference;
import ao.co.oportunidade.order.model.PackageType;
import ao.co.oportunidade.notification.service.AlertService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.jboss.logging.Logger;

import java.time.temporal.ChronoUnit;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for processing payment webhooks and managing payment lifecycle.
 */
@ApplicationScoped
public class PaymentProcessService extends
        BasicApplicationService<PaymentTransaction, PaymentTransactionRepository,
                Order, OrderRepository, PaymentTransactionService, OrderService> {

    private static final Logger LOG = Logger.getLogger(PaymentProcessService.class);

    private final OdooPaymentService odooPaymentService;

    @Inject
    DocumentTokenService tokenService;

    @Inject
    NotificationService notificationService;

    @Inject
    AlertService alertService;

    public PaymentProcessService(OdooPaymentService odooPaymentService) {
        this.odooPaymentService = odooPaymentService;
    }

    /**
     * Process the AppyPay webhook payload.
     * Routes to appropriate handler based on payment status.
     *
     * @param payload the webhook payload
     */
    @Transactional
    public void processPaymentStatus(final AppyPayWebhookPayload payload) throws RuntimeException {
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
        final Order order = orderService.find(payload);

        // Phase 1 Enhancement: Enrich order with employer and document mapping
        enrichOrderWithEmployerInfo(order, payload);

        order.setStatus(Order.OrderStatus.COMPLETED);  // Changed from PAID to COMPLETED
        orderService.transact(order);

        final PaymentTransaction paymentTransaction = createPaymentTransaction(
                payload, order, PaymentTransaction.TransactionStatus.SUCCESS);
        sendToOdooWithRetry(paymentTransaction);

        // ========== PHASE 1: GENERATE ACCESS TOKEN ==========
        try {
            if (order.getEmployerId() != null &&
                    order.getCandidateIds() != null &&
                    !order.getCandidateIds().isEmpty()) {

                // Generate JWT token for document access
                String accessToken = tokenService.generateAccessToken(order);

                // Generate download URL with token
                String downloadUrl = tokenService.generateDownloadUrl(accessToken);

                LOG.infof("Generated access token for order %s, employer %s",
                        order.getId(), order.getEmployerId());

                // Send email notification to employer
                notificationService.sendDocumentAccessEmail(
                        order.getEmployerEmail(),
                        downloadUrl,
                        order.getPackageType(),
                        order.getCandidateIds().size()
                );

                LOG.infof("Access notification sent to: %s", order.getEmployerEmail());

            } else {
                LOG.warnf("Cannot generate token - missing employer or candidate info for order: %s",
                        order.getMerchantTransactionId());
            }
        } catch (Exception e) {
            LOG.errorf(e, "Failed to generate access token for order: %s",
                    order.getMerchantTransactionId());
            // Send alert to admin
            alertService.sendTokenGenerationAlert(order.getId(), e.getMessage());
            // Don't fail the payment, but log the error
            // Admin will need to manually generate token
        }
        // ====================================================

        LOG.infof("Successfully processed payment for order: %s",
                order.getMerchantTransactionId());
    }

    /**
     * Enrich order with employer information and document mapping.
     * Phase 1 implementation: Looks up employer from reference code.
     *
     * @param order Order to enrich
     * @param payload Webhook payload with reference code
     */
    private void enrichOrderWithEmployerInfo(Order order, AppyPayWebhookPayload payload) {
        // Extract reference code from payload
        String referenceCode = extractReferenceCode(payload);

        if (referenceCode == null) {
            LOG.warnf("No reference code found in payload for order: %s",
                    order.getMerchantTransactionId());
            return;
        }

        // Lookup employer from reference code
        EmployerReference employer = EmployerReference.findByReferenceCode(referenceCode);

        if (employer == null) {
            LOG.errorf("No employer found for reference: %s (order: %s)",
                    referenceCode, order.getMerchantTransactionId());
            // Send alert to admin
            alertService.sendEmployerReferenceNotFoundAlert(referenceCode, payload.getId());
            throw new IllegalStateException(
                    "Invalid employer reference: " + referenceCode);
        }

        // Enrich order with employer info
        order.setEmployerId(employer.getEmployerId());
        order.setEmployerEmail(employer.getEmployerEmail());
        order.setReferenceCode(referenceCode);

        // Set package type (from payload or default to STANDARD)
        order.setPackageType(determinePackageType(payload));

        // Set candidate IDs (from payload or extract from order details)
        order.setCandidateIds(extractCandidateIds(payload));

        // Map candidate IDs to Odoo document IDs
        order.setOdooDocumentIds(mapCandidatesToOdooDocuments(order.getCandidateIds()));

        LOG.infof("Order enriched: employer=%s, package=%s, candidates=%d",
                order.getEmployerId(), order.getPackageType(),
                order.getCandidateIds() != null ? order.getCandidateIds().size() : 0);
    }

    /**
     * Send payment to Odoo with automatic retry on failure.
     * Retries up to 3 times with exponential backoff.
     *
     * @param paymentTransaction the payment transaction to send
     */
    @Retry(
            maxRetries = 3,
            delay = 1,
            delayUnit = ChronoUnit.SECONDS,
            maxDuration = 30,
            durationUnit = ChronoUnit.SECONDS,
            jitter = 200
    )
    @Timeout(value = 10, unit = ChronoUnit.SECONDS)
    protected void sendToOdooWithRetry(PaymentTransaction paymentTransaction) {
        try {
            LOG.infof("Sending payment to Odoo: %s", paymentTransaction.getAppypayTransactionId());
            odooPaymentService.sendPaymentToOdoo(paymentTransaction);
            LOG.infof("Successfully sent payment to Odoo: %s", paymentTransaction.getAppypayTransactionId());
        } catch (Exception e) {
            LOG.warnf(e, "Failed to send payment to Odoo: %s (will retry)",
                    paymentTransaction.getAppypayTransactionId());
            // Send alert on each failure
            alertService.sendOdooApiFailureAlert("sendPaymentToOdoo", e.getMessage());
            throw e; // Re-throw to trigger retry
        }
    }

    /**
     * Extract reference code from webhook payload.
     *
     * @param payload Webhook payload
     * @return Reference code or null if not found
     */
    private String extractReferenceCode(AppyPayWebhookPayload payload) {
        if (payload.getReference() != null) {
            return payload.getReference().getReferenceNumber();
        }
        return null;
    }

    /**
     * Determine package type from payload.
     *
     * @param payload Webhook payload
     * @return PackageType (defaults to STANDARD if not specified)
     */
    private PackageType determinePackageType(AppyPayWebhookPayload payload) {
        // TODO: Extract from payload custom fields or amount-based logic
        // For Phase 1, default to STANDARD
        // In Phase 2, implement based on business rules:
        // - Parse from merchantTransactionId pattern
        // - Map from payment amount
        // - Use custom field in webhook payload
        return PackageType.STANDARD;
    }

    /**
     * Extract candidate IDs from webhook payload.
     *
     * @param payload Webhook payload
     * @return List of candidate IDs
     */
    private List<String> extractCandidateIds(AppyPayWebhookPayload payload) {
        // TODO Phase 2: Extract from payload custom fields
        // For Phase 1, return sample data
        // In production, this should come from:
        // - Custom fields in AppyPay payload
        // - Lookup based on merchantTransactionId
        // - Pre-stored in order creation
        LOG.warnf("Using placeholder candidate IDs for order: %s",
                payload.getMerchantTransactionId());
        return List.of("CAND-001", "CAND-002");  // Placeholder
    }

    /**
     * Map candidate IDs to their Odoo document IDs.
     *
     * @param candidateIds List of candidate IDs
     * @return List of Odoo document IDs (ir.attachment)
     */
    private List<String> mapCandidatesToOdooDocuments(List<String> candidateIds) {
        // TODO Phase 2: Query Odoo API to get actual document IDs
        // For Phase 1, use placeholder mapping
        // In production:
        // - Call odooClient.getCandidateDocuments(candidateId)
        // - Extract document IDs from results
        // - Handle missing/invalid candidates
        return candidateIds.stream()
                .map(id -> "odoo_doc_" + id)
                .toList();
    }

    private void handlePendingPayment(final AppyPayWebhookPayload payload) {
        LOG.infof("Handling pending payment: %s", payload.getId());

        final Order order = getSupportingDomainService().find(payload);
        order.setStatus(Order.OrderStatus.PENDING);
        getSupportingDomainService().transact(order);

        final PaymentTransaction paymentTransaction = createPaymentTransaction(payload, order, PaymentTransaction.TransactionStatus.PENDING);
        odooPaymentService.sendPaymentToOdoo(paymentTransaction);

        LOG.infof("Payment pending for order: %s", order.getMerchantTransactionId());
    }

    private void handleFailedPayment(final AppyPayWebhookPayload payload) {
        LOG.infof("Handling failed payment: %s", payload.getId());

        final Order order = getSupportingDomainService().find(payload);
        order.setStatus(Order.OrderStatus.FAILED);
        getSupportingDomainService().transact(order);

        final PaymentTransaction transaction = createPaymentTransaction(
                payload, order, PaymentTransaction.TransactionStatus.FAILED);

        if (payload.getResponseStatus() != null) {
            transaction.setErrorMessage(payload.getResponseStatus().getMessage());
            getMainDomainService().transact(transaction);
        }

        // Failed payments are recorded locally only - no Odoo notification needed
        LOG.infof("Payment failed for order: %s", order.getMerchantTransactionId());
    }

    private void handleCancelledPayment(final AppyPayWebhookPayload payload) {
        LOG.infof("Handling cancelled payment: %s", payload.getId());

        final Optional<Order> existingOrder = getSupportingDomainService()
                .findByMerchantTransactionId(payload.getMerchantTransactionId());

        if (existingOrder.isPresent()) {
            final                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      Order order = existingOrder.get();
            order.setStatus(Order.OrderStatus.CANCELLED);
            getSupportingDomainService().transact(order);

            final PaymentTransaction paymentTransaction = createPaymentTransaction(payload, order, PaymentTransaction.TransactionStatus.CANCELLED);
            odooPaymentService.sendPaymentToOdoo(paymentTransaction);

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

        getMainDomainService().transact(transaction);
        LOG.infof("Created payment transaction: %s for order: %s",
                transaction.getId(), order.getId());

        return transaction;
    }
}