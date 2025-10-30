package ao.co.oportunidade.webhook;

import ao.co.oportunidade.DomainService;
import ao.co.oportunidade.webhook.dto.AppyPayWebhookPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@ApplicationScoped
public class WebhookEventService extends DomainService<WebhookEvent, WebhookEventRepository> {

    private final ObjectMapper objectMapper;

    @Inject
    public WebhookEventService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Collection<WebhookEvent> getAllDomains() {
        return getRepository().findDomains();
    }

    @Override
    public void saveDomain(WebhookEvent event) {
        try {
            validateDomain(event);
        } catch (ao.co.oportunidade.DomainNotCreatedException e) {
            throw new RuntimeException("Failed to create webhook event", e);
        }
        getRepository().save(event);
    }

    /**
     * Find webhook event by AppyPay transaction ID.
     *
     * @param appypayTransactionId the AppyPay transaction ID
     * @return Optional containing the webhook event if found
     */
    public java.util.Optional<WebhookEvent> findByAppyPayTransactionId(String appypayTransactionId) {
        return getRepository().findByAppyPayTransactionId(appypayTransactionId);
    }

    /**
     * Find webhook events by processing status.
     *
     * @param status the processing status
     * @return list of webhook events with the given status
     */
    public List<WebhookEvent> findByStatus(WebhookEvent.ProcessingStatus status) {
        return getRepository().findByStatus(status);
    }

    /**
     * Check if a webhook has already been processed (idempotency check).
     *
     * @param appypayTransactionId the AppyPay transaction ID
     * @return true if already processed, false otherwise
     */
    public boolean isAlreadyProcessed(String appypayTransactionId) {
        return findByAppyPayTransactionId(appypayTransactionId)
                .map(event -> event.getProcessingStatus() == WebhookEvent.ProcessingStatus.PROCESSED ||
                        event.getProcessingStatus() == WebhookEvent.ProcessingStatus.PROCESSING)
                .orElse(false);
    }

    /**
     * Update webhook event status.
     *
     * @param event the webhook event to update
     */
    public void updateEvent(WebhookEvent event) {
        getRepository().save(event); // Will persist/merge based on state
    }

    public WebhookEvent createWebhookEvent(AppyPayWebhookPayload payload) {

        final WebhookEvent event = new WebhookEvent();
        event.setAppypayTransactionId(payload.getId());
        event.setMerchantTransactionId(payload.getMerchantTransactionId());
        event.setWebhookType(payload.getType());
        event.setProcessingStatus(WebhookEvent.ProcessingStatus.RECEIVED);
        event.setRetryCount(0);
        event.setReceivedAt(Instant.now());

        // Store the full payload as JSON string
        try {
            event.setPayload(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize payload", e);
        }
        return event;
    }
}