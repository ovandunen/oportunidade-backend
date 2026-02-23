package ao.co.oportunidade.webhook.service;

import ao.co.oportunidade.notification.service.AlertService;
import ao.co.oportunidade.payment.service.PaymentProcessService;
import ao.co.oportunidade.webhook.dto.AppyPayWebhookPayload;
import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import java.time.temporal.ChronoUnit;

/**
 * Async processor for webhook events using SmallRye Reactive Messaging.
 * Processes webhooks asynchronously after immediate HTTP response.
 * Includes automatic retry logic and admin alerting on failures.
 * 
 * Fault Tolerance Strategy:
 * - @Retry: Automatically retries failed operations (max 3 attempts)
 * - @Timeout: Fails operation if it takes longer than 30 seconds
 * - @Fallback: Sends admin alert when all retries are exhausted
 */
@ApplicationScoped
public class WebhookProcessor {

    private static final Logger LOG = Logger.getLogger(WebhookProcessor.class);

    @Inject
    PaymentProcessService paymentProcessService;

    @Inject
    WebhookEventServiceFacade webhookEventService;
    
    @Inject
    AlertService alertService;

    /**
     * Process incoming webhook messages asynchronously with fault tolerance.
     * This method is triggered when a message is sent to the "webhook-events" channel.
     * 
     * Fault Tolerance:
     * - Retries up to 3 times with exponential backoff
     * - Times out after 30 seconds per attempt
     * - Sends admin alert if all retries fail
     *
     * @param payload the webhook payload to process
     */
    @Incoming("webhook-events-in")
    @Blocking
    @Retry(
        maxRetries = 3,
        delay = 2,
        delayUnit = ChronoUnit.SECONDS,
        maxDuration = 60,
        durationUnit = ChronoUnit.SECONDS,
        jitter = 500,
        retryOn = {RuntimeException.class, Exception.class}
    )
    @Timeout(value = 30, unit = ChronoUnit.SECONDS)
    @Fallback(fallbackMethod = "fallbackProcessPayment")
    public void processPayment(AppyPayWebhookPayload payload) {
        final String transactionId = payload.getId();
        LOG.infof("Starting async processing of webhook: %s", transactionId);

        try {
            // Mark as processing
            webhookEventService.markAsProcessing(transactionId);

            // Process the payment
            paymentProcessService.processPaymentStatus(payload);

            // Mark as processed
            webhookEventService.markAsProcessed(transactionId);

            LOG.infof("Successfully processed webhook: %s", transactionId);

        } catch (Exception e) {
            LOG.errorf(e, "Failed to process webhook: %s (will retry)", transactionId);
            
            // Mark as failed (will be retried)
            webhookEventService.markAsFailed(transactionId, e.getMessage());

            // Re-throw to trigger retry mechanism
            throw new RuntimeException("Webhook processing failed", e);
        }
    }

    /**
     * Fallback method called when all retries are exhausted.
     * Sends critical alert to admin team for manual intervention.
     * 
     * @param payload the webhook payload that failed
     */
    public void fallbackProcessPayment(AppyPayWebhookPayload payload) {
        final String transactionId = payload.getId();
        
        LOG.errorf("All retry attempts exhausted for webhook: %s - sending admin alert", transactionId);
        
        try {
            // Send critical alert
            alertService.sendWebhookFailureAlert(
                transactionId,
                "All retry attempts exhausted. Manual investigation required.",
                3 // max retries
            );
            
            // Mark as permanently failed
            webhookEventService.markAsFailed(
                transactionId, 
                "PERMANENT_FAILURE: All retry attempts exhausted"
            );
            
        } catch (Exception alertException) {
            // If even the alert fails, just log it
            LOG.errorf(alertException, "Failed to send alert for failed webhook: %s", transactionId);
        }
        
        // Log final failure details
        LOG.errorf(
            "CRITICAL: Webhook %s processing failed permanently. " +
            "Payment status: %s, Amount: %s, Customer: %s",
            transactionId,
            payload.getStatus(),
            payload.getAmount(),
            payload.getCustomer() != null ? payload.getCustomer().getEmail() : "unknown"
        );
    }
}
