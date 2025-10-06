package ao.co.oportunidade.webhook.entity;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for PaymentTransaction entity operations.
 */
@ApplicationScoped
public class PaymentTransactionRepository implements PanacheRepositoryBase<PaymentTransaction, UUID> {

    /**
     * Find payment transaction by AppyPay transaction ID.
     *
     * @param appypayTransactionId the AppyPay transaction ID
     * @return Optional containing the transaction if found
     */
    public Optional<PaymentTransaction> findByAppyPayTransactionId(String appypayTransactionId) {
        return find("appypayTransactionId", appypayTransactionId).firstResultOptional();
    }

    /**
     * Find all transactions for a specific order.
     *
     * @param orderId the order ID
     * @return list of transactions for the order
     */
    public List<PaymentTransaction> findByOrderId(UUID orderId) {
        return find("orderId", orderId).list();
    }

    /**
     * Find transactions by status.
     *
     * @param status the transaction status
     * @return list of transactions with the given status
     */
    public List<PaymentTransaction> findByStatus(PaymentTransaction.TransactionStatus status) {
        return find("status", status).list();
    }

    /**
     * Check if a transaction exists with the given AppyPay transaction ID.
     *
     * @param appypayTransactionId the AppyPay transaction ID
     * @return true if exists, false otherwise
     */
    public boolean existsByAppyPayTransactionId(String appypayTransactionId) {
        return count("appypayTransactionId", appypayTransactionId) > 0;
    }
}
