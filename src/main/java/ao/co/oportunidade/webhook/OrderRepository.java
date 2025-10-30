package ao.co.oportunidade.webhook;

import ao.co.oportunidade.Repository;
import ao.co.oportunidade.webhook.entity.OrderEntity;
import ao.co.oportunidade.webhook.entity.OrderEntityMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Order domain following DDD principles.
 */
@ApplicationScoped
public class OrderRepository extends Repository<Order, OrderEntity, OrderEntityMapper> {

    @Inject
    OrderEntityMapper mapper;

    @Override
    protected Collection<Order> findDomains() {
        final List<OrderEntity> entities = getEntityManager()
                .createNamedQuery(OrderEntity.FIND_ALL, OrderEntity.class)
                .getResultStream()
                .toList();
        return entities.stream()
                .map(mapper::mapToDomain)
                .toList();
    }

    @Override
    public Optional<Order> findDomainById(Order domain) {
        try {
            final OrderEntity entity = getEntityManager()
                    .createNamedQuery(OrderEntity.FIND_BY_ID, OrderEntity.class)
                    .setParameter(OrderEntity.PRIMARY_KEY, domain.getId())
                    .getSingleResult();
            return Optional.ofNullable(mapper.mapToDomain(entity));
        } catch (Exception e) {
            return Optional.empty();
        }
    }



    /**
     * Find order by merchant transaction ID.
     *
     * @param merchantTransactionId the merchant transaction ID
     * @return Optional containing the order if found
     */
    public Optional<Order> findByMerchantTransactionId(String merchantTransactionId) {
        try {
            final List<OrderEntity> results = getEntityManager()
                    .createNamedQuery(OrderEntity.FIND_BY_MERCHANT_TX_ID, OrderEntity.class)
                    .setParameter("merchantTxId", merchantTransactionId)
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
     * Find order by reference ID.
     *
     * @param referenceId the reference ID
     * @return Optional containing the order if found
     */
    public Optional<Order> findByReferenceId(java.util.UUID referenceId) {
        try {
            List<OrderEntity> results = getEntityManager()
                    .createQuery("SELECT o FROM OrderEntity o WHERE o.referenceId = :refId", OrderEntity.class)
                    .setParameter("refId", referenceId)
                    .getResultList();
            
            if (results.isEmpty()) {
                return Optional.empty();
            }
            
            return Optional.ofNullable(mapper.mapToDomain(results.getFirst()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
