package ao.co.oportunidade.webhook.entity;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for WebhookEvent entity operations.
 */
@ApplicationScoped
public class WebhookEventRepository implements PanacheRepositoryBase<WebhookEvent, UUID> {

    /**
     * Find webhook event by AppyPay transaction ID.
     *
     * @param appypayTransactionId the AppyPay transaction ID
     * @return Optional containing the webhook event if found
     */
    public Optional<WebhookEvent> findByAppyPayTransactionId(String appypayTransactionId) {
        return find("appypayTransactionId", appypayTransactionId).firstResultOptional();
    }

    /**
     * Find webhook events by processing status.
     *
     * @param status the processing status
     * @return list of webhook events with the given status
     */
    public List<WebhookEvent> findByStatus(WebhookEvent.ProcessingStatus status) {
        return find("processingStatus", status).list();
    }

    /**
     * Find failed webhook events that need retry.
     *
     * @param maxRetries maximum retry count
     * @param olderThan timestamp to find events older than
     * @return list of webhook events eligible for retry
     */
    public List<WebhookEvent> findFailedEventsForRetry(int maxRetries, Instant olderThan) {
        return find("processingStatus = ?1 and retryCount < ?2 and updatedDate < ?3",
                WebhookEvent.ProcessingStatus.FAILED, maxRetries, olderThan)
                .list();
    }

    /**
     * Check if a webhook event exists with the given AppyPay transaction ID.
     *
     * @param appypayTransactionId the AppyPay transaction ID
     * @return true if exists, false otherwise
     */
    public boolean existsByAppyPayTransactionId(String appypayTransactionId) {
        return count("appypayTransactionId", appypayTransactionId) > 0;
    }

    /**
     * Find events in dead letter queue.
     *
     * @return list of dead letter events
     */
    public List<WebhookEvent> findDeadLetterEvents() {
        return findByStatus(WebhookEvent.ProcessingStatus.DEAD_LETTER);
    }
}
