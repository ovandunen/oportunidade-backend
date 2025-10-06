package ao.co.oportunidade.webhook.service;

import ao.co.oportunidade.webhook.dto.AppyPayWebhookPayload;
import ao.co.oportunidade.webhook.entity.WebhookEvent;
import ao.co.oportunidade.webhook.entity.WebhookEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Optional;

/**
 * Service for managing webhook events and ensuring idempotency.
 */
@ApplicationScoped
public class WebhookEventService {

    private static final Logger LOG = Logger.getLogger(WebhookEventService.class);

    @Inject
    WebhookEventRepository webhookEventRepository;

    @Inject
    ObjectMapper objectMapper;

    /**
     * Check if a webhook has already been processed (idempotency check).
     *
     * @param appypayTransactionId the AppyPay transaction ID
     * @return true if already processed, false otherwise
     */
    public boolean isAlreadyProcessed(String appypayTransactionId) {
        Optional<WebhookEvent> existing = webhookEventRepository.findByAppyPayTransactionId(appypayTransactionId);
        
        if (existing.isPresent()) {
            WebhookEvent event = existing.get();
            WebhookEvent.ProcessingStatus status = event.getProcessingStatus();
            
            // Consider PROCESSED and PROCESSING as already handled
            if (status == WebhookEvent.ProcessingStatus.PROCESSED || 
                status == WebhookEvent.ProcessingStatus.PROCESSING) {
                LOG.infof("Webhook already processed or in progress: %s", appypayTransactionId);
                return true;
            }
        }
        
        return false;
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
            
            WebhookEvent event = WebhookEvent.builder()
                    .appypayTransactionId(payload.getId())
                    .merchantTransactionId(payload.getMerchantTransactionId())
                    .webhookType(payload.getType())
                    .processingStatus(WebhookEvent.ProcessingStatus.RECEIVED)
                    .payload(payloadJson)
                    .receivedAt(Instant.now())
                    .retryCount(0)
                    .build();
            
            webhookEventRepository.persist(event);
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
        webhookEventRepository.findByAppyPayTransactionId(eventId).ifPresent(event -> {
            event.setProcessingStatus(WebhookEvent.ProcessingStatus.PROCESSING);
            webhookEventRepository.persist(event);
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
        webhookEventRepository.findByAppyPayTransactionId(eventId).ifPresent(event -> {
            event.setProcessingStatus(WebhookEvent.ProcessingStatus.PROCESSED);
            event.setProcessedAt(Instant.now());
            webhookEventRepository.persist(event);
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
        webhookEventRepository.findByAppyPayTransactionId(eventId).ifPresent(event -> {
            event.setProcessingStatus(WebhookEvent.ProcessingStatus.FAILED);
            event.setErrorMessage(errorMessage);
            event.setRetryCount(event.getRetryCount() + 1);
            webhookEventRepository.persist(event);
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
        webhookEventRepository.findByAppyPayTransactionId(eventId).ifPresent(event -> {
            event.setProcessingStatus(WebhookEvent.ProcessingStatus.DEAD_LETTER);
            webhookEventRepository.persist(event);
            LOG.errorf("Moved webhook event to dead letter queue: %s", eventId);
        });
    }
}
