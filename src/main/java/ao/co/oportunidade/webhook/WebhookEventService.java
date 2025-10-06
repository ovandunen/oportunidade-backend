package ao.co.oportunidade.webhook;

import ao.co.oportunidade.DomainService;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Collection;
import java.util.List;

/**
 * Service for WebhookEvent domain following DDD principles.
 */
@ApplicationScoped
public class WebhookEventService extends DomainService<WebhookEvent, WebhookEventRepository> {

    @Override
    protected Collection<WebhookEvent> getAllDomains() {
        return getRepository().findDomains();
    }

    @Override
    protected void createDomain(WebhookEvent event) {
        try {
            validateDomain(event);
        } catch (ao.co.oportunidade.DomainNotCreatedException e) {
            throw new RuntimeException("Failed to create webhook event", e);
        }
        getRepository().createDomain(event);
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
        getRepository().createDomain(event); // Will persist/merge based on state
    }
}
