package ao.co.oportunidade.webhook;

import ao.co.oportunidade.DomainService;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Collection;

/**
 * Service for Order domain following DDD principles.
 */
@ApplicationScoped
public class OrderService extends DomainService<Order, OrderRepository> {

    @Override
    protected Collection<Order> getAllDomains() {
        return getRepository().findDomains();
    }

    @Override
    protected void createDomain(Order order) {
        try {
            validateDomain(order);
        } catch (ao.co.oportunidade.DomainNotCreatedException e) {
            throw new RuntimeException("Failed to create order", e);
        }
        getRepository().createDomain(order);
    }

    /**
     * Find order by merchant transaction ID.
     *
     * @param merchantTransactionId the merchant transaction ID
     * @return Optional containing the order if found
     */
    public java.util.Optional<Order> findByMerchantTransactionId(String merchantTransactionId) {
        return getRepository().findByMerchantTransactionId(merchantTransactionId);
    }

    /**
     * Update an existing order.
     *
     * @param order the order to update
     */
    public void updateOrder(Order order) {
        getRepository().getEntityManager().merge(
            getRepository().findById(order.getId())
                .orElseThrow(() -> new RuntimeException("Order not found"))
        );
        getRepository().createDomain(order); // Will persist/merge based on state
    }
}
