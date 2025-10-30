package ao.co.oportunidade.webhook.service;

import ao.co.oportunidade.webhook.WebhookEvent;
import ao.co.oportunidade.webhook.WebhookEventService;
import ao.co.oportunidade.webhook.dto.AppyPayWebhookPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for managing webhook events and ensuring idempotency.
 * Refactored to use DDD domain service.
 */
@ApplicationScoped
public class WebhookEventServiceFacade {

    private static final Logger LOG = Logger.getLogger(WebhookEventServiceFacade.class);

    @Inject
    WebhookEventService webhookEventService;

    @Inject
    ObjectMapper objectMapper;

    /**
     * Check if a webhook has already been processed (idempotency check).
     *
     * @param appypayTransactionId the AppyPay transaction ID
     * @return true if already processed, false otherwise
     */
    public boolean isAlreadyProcessed(String appypayTransactionId) {
        return webhookEventService.isAlreadyProcessed(appypayTransactionId);
    }

    /**
     * Create a new webhook event record.
     *
     * @param payload the webhook payload
     * @return the created webhook event
     */
    @Transactional
    public WebhookEvent createWebhookEvent(AppyPayWebhookPayload payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            
            WebhookEvent event = new WebhookEvent();
            event.setId(UUID.randomUUID());
            event.setAppypayTransactionId(payload.getId());
            event.setMerchantTransactionId(payload.getMerchantTransactionId());
            event.setWebhookType(payload.getType());
            event.setProcessingStatus(WebhookEvent.ProcessingStatus.RECEIVED);
            event.setPayload(payloadJson);
            event.setReceivedAt(Instant.now());
            event.setRetryCount(0);
            event.setCreatedDate(Instant.now());
            event.setUpdatedDate(Instant.now());
            
            webhookEventService.saveDomain(event);
            LOG.infof("Created webhook event: %s for transaction: %s", 
                    event.getId(), payload.getId());
            
            return event;
        } catch (JsonProcessingException e) {
            LOG.errorf(e, "Failed to serialize webhook payload: %s", payload.getId());
            throw new RuntimeException("Failed to create webhook event", e);
        }
    }

    /**
     * Update webhook event status to processing.
     *
     * @param eventId the webhook event ID
     */
    @Transactional
    public void markAsProcessing(String eventId) {
        webhookEventService.findByAppyPayTransactionId(eventId).ifPresent(event -> {
            event.setProcessingStatus(WebhookEvent.ProcessingStatus.PROCESSING);
            webhookEventService.updateEvent(event);
            LOG.infof("Marked webhook event as processing: %s", eventId);
        });
    }

    /**
     * Update webhook event status to processed.
     *
     * @param eventId the webhook event ID
     */
    @Transactional
    public void markAsProcessed(String eventId) {
        webhookEventService.findByAppyPayTransactionId(eventId).ifPresent(event -> {
            event.setProcessingStatus(WebhookEvent.ProcessingStatus.PROCESSED);
            event.setProcessedAt(Instant.now());
            webhookEventService.updateEvent(event);
            LOG.infof("Marked webhook event as processed: %s", eventId);
        });
    }

    /**
     * Update webhook event status to failed.
     *
     * @param eventId the webhook event ID
     * @param errorMessage the error message
     */
    @Transactional
    public void markAsFailed(String eventId, String errorMessage) {
        webhookEventService.findByAppyPayTransactionId(eventId).ifPresent(event -> {
            event.setProcessingStatus(WebhookEvent.ProcessingStatus.FAILED);
            event.setErrorMessage(errorMessage);
            event.setRetryCount(event.getRetryCount() + 1);
            webhookEventService.updateEvent(event);
            LOG.errorf("Marked webhook event as failed: %s, error: %s", eventId, errorMessage);
        });
    }

    /**
     * Move webhook event to dead letter queue after max retries.
     *
     * @param eventId the webhook event ID
     */
    @Transactional
    public void moveToDeadLetter(String eventId) {
        webhookEventService.findByAppyPayTransactionId(eventId).ifPresent(event -> {
            event.setProcessingStatus(WebhookEvent.ProcessingStatus.DEAD_LETTER);
            webhookEventService.updateEvent(event);
            LOG.errorf("Moved webhook event to dead letter queue: %s", eventId);
        });
    }
}
