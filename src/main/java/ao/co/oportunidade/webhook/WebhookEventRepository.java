package ao.co.oportunidade.webhook;

import solutions.envision.entity.Repository;
import ao.co.oportunidade.webhook.entity.WebhookEventEntity;
import ao.co.oportunidade.webhook.entity.WebhookEventEntityMapper;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository for WebhookEvent domain following DDD principles.
 */
@ApplicationScoped
public class WebhookEventRepository extends Repository<WebhookEvent, WebhookEventEntity,WebhookEventEntityMapper> {


    @Override
    protected Collection<WebhookEvent> findDomains() {
        final List<WebhookEventEntity> entities = getEntityManager()
                .createNamedQuery(WebhookEventEntity.FIND_ALL, WebhookEventEntity.class)
                .getResultStream()
                .toList();
        return entities.stream()
                .map(getMapper()::mapToDomain)
                .toList();
    }

    @Override
    public Optional<WebhookEvent> findDomainById(WebhookEvent domain) {
        try {
            final WebhookEventEntity entity = getEntityManager()
                    .createNamedQuery(WebhookEventEntity.FIND_BY_ID, WebhookEventEntity.class)
                    .setParameter(WebhookEventEntity.PRIMARY_KEY, domain.getId())
                    .getSingleResult();
            return Optional.ofNullable(getMapper().mapToDomain(entity));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Find webhook event by AppyPay transaction ID.
     *
     * @param appypayTransactionId the AppyPay transaction ID
     * @return Optional containing the webhook event if found
     */
    public Optional<WebhookEvent> findByAppyPayTransactionId(String appypayTransactionId) {
        try {
            List<WebhookEventEntity> results = getEntityManager()
                    .createNamedQuery(WebhookEventEntity.FIND_BY_APPYPAY_TX_ID, WebhookEventEntity.class)
                    .setParameter("appypayTxId", appypayTransactionId)
                    .getResultList();
            
            if (results.isEmpty()) {
                return Optional.empty();
            }
            
            return Optional.ofNullable(getMapper().mapToDomain(results.getFirst()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Find webhook events by processing status.
     *
     * @param status the processing status
     * @return list of webhook events with the given status
     */
    public List<WebhookEvent> findByStatus(WebhookEvent.ProcessingStatus status) {
        final List<WebhookEventEntity> entities = getEntityManager()
                .createQuery("SELECT we FROM WebhookEventEntity we WHERE we.processingStatus = :status", WebhookEventEntity.class)
                .setParameter("status", status.name())
                .getResultList();
        return entities.stream()
                .map(getMapper()::mapToDomain)
                .toList();
    }
}
