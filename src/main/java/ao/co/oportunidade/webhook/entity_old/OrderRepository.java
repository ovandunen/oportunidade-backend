package ao.co.oportunidade.webhook.entity;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Order entity operations.
 */
@ApplicationScoped
public class OrderRepository implements PanacheRepositoryBase<Order, UUID> {

    /**
     * Find order by merchant transaction ID.
     *
     * @param merchantTransactionId the merchant transaction ID
     * @return Optional containing the order if found
     */
    public Optional<Order> findByMerchantTransactionId(String merchantTransactionId) {
        return find("merchantTransactionId", merchantTransactionId).firstResultOptional();
    }

    /**
     * Find order by reference ID.
     *
     * @param referenceId the reference ID
     * @return Optional containing the order if found
     */
    public Optional<Order> findByReferenceId(UUID referenceId) {
        return find("referenceId", referenceId).firstResultOptional();
    }

    /**
     * Check if an order exists with the given merchant transaction ID.
     *
     * @param merchantTransactionId the merchant transaction ID
     * @return true if exists, false otherwise
     */
    public boolean existsByMerchantTransactionId(String merchantTransactionId) {
        return count("merchantTransactionId", merchantTransactionId) > 0;
    }
}
