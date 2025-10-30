package ao.co.oportunidade.webhook;

import ao.co.oportunidade.Repository;
import ao.co.oportunidade.webhook.entity.PaymentTransactionEntity;
import ao.co.oportunidade.webhook.entity.PaymentTransactionEntityMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for PaymentTransaction domain following DDD principles.
 */
@ApplicationScoped
public class PaymentTransactionRepository extends Repository<PaymentTransaction, PaymentTransactionEntity,PaymentTransactionEntityMapper> {

    @Inject
    PaymentTransactionEntityMapper mapper;

    @Override
    protected Collection<PaymentTransaction> findDomains() {
        final List<PaymentTransactionEntity> entities = getEntityManager()
                .createNamedQuery(PaymentTransactionEntity.FIND_ALL, PaymentTransactionEntity.class)
                .getResultStream()
                .toList();
        return entities.stream()
                .map(mapper::mapToDomain)
                .toList();
    }

    @Override
    public Optional<PaymentTransaction> findDomainById(PaymentTransaction domain) {
        try {
            final PaymentTransactionEntity entity = getEntityManager()
                    .createNamedQuery(PaymentTransactionEntity.FIND_BY_ID, PaymentTransactionEntity.class)
                    .setParameter(PaymentTransactionEntity.PRIMARY_KEY, domain.getId())
                    .getSingleResult();
            return Optional.ofNullable(mapper.mapToDomain(entity));
        } catch (Exception e) {
            return Optional.empty();
        }
    }



    /**
     * Find payment transaction by AppyPay transaction ID.
     *
     * @param appypayTransactionId the AppyPay transaction ID
     * @return Optional containing the transaction if found
     */
    public Optional<PaymentTransaction> findByAppyPayTransactionId(String appypayTransactionId) {
        try {
            List<PaymentTransactionEntity> results = getEntityManager()
                    .createNamedQuery(PaymentTransactionEntity.FIND_BY_APPYPAY_TX_ID, PaymentTransactionEntity.class)
                    .setParameter("appypayTxId", appypayTransactionId)
                    .getResultList();
            
            if (results.isEmpty()) {
                return Optional.empty();
            }
            
            return Optional.ofNullable(mapper.mapToDomain(results.getFirst()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Find all transactions for a specific order.
     *
     * @param orderId the order ID
     * @return list of transactions for the order
     */
    public List<PaymentTransaction> findByOrderId(UUID orderId) {
        final List<PaymentTransactionEntity> entities = getEntityManager()
                .createQuery("SELECT pt FROM PaymentTransactionEntity pt WHERE pt.orderId = :orderId", PaymentTransactionEntity.class)
                .setParameter("orderId", orderId)
                .getResultList();
        return entities.stream()
                .map(mapper::mapToDomain)
                .toList();
    }
}
