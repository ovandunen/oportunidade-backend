package ao.co.oportunidade.webhook.service;

import ao.co.oportunidade.webhook.dto.AppyPayWebhookPayload;
import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

/**
 * Async processor for webhook events using SmallRye Reactive Messaging.
 * Processes webhooks asynchronously after immediate HTTP response.
 */
@ApplicationScoped
public class WebhookProcessor {

    private static final Logger LOG = Logger.getLogger(WebhookProcessor.class);

    @Inject
    PaymentService paymentService;

    @Inject
    WebhookEventService webhookEventService;

    /**
     * Process incoming webhook messages asynchronously.
     * This method is triggered when a message is sent to the "webhook-events" channel.
     *
     * @param payload the webhook payload to process
     */
    @Incoming("webhook-events")
    @Blocking
    public void processWebhook(AppyPayWebhookPayload payload) {
        String transactionId = payload.getId();
        LOG.infof("Starting async processing of webhook: %s", transactionId);

        try {
            // Mark as processing
            webhookEventService.markAsProcessing(transactionId);

            // Process the payment
            paymentService.processWebhook(payload);

            // Mark as processed
            webhookEventService.markAsProcessed(transactionId);

            LOG.infof("Successfully processed webhook: %s", transactionId);

        } catch (Exception e) {
            LOG.errorf(e, "Failed to process webhook: %s", transactionId);
            
            // Mark as failed
            webhookEventService.markAsFailed(transactionId, e.getMessage());

            // Could implement retry logic here or move to dead letter queue
            throw new RuntimeException("Webhook processing failed", e);
        }
    }
}
