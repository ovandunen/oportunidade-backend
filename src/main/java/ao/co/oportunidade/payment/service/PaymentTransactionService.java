package ao.co.oportunidade.payment.service;

import ao.co.oportunidade.payment.entity.PaymentTransactionRepository;
import ao.co.oportunidade.payment.model.PaymentTransaction;
import solutions.envision.service.DomainService;
import jakarta.enterprise.context.ApplicationScoped;
import solutions.envision.model.DomainNotCreatedException;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Service for PaymentTransaction domain following DDD principles.
 */
@ApplicationScoped
public class PaymentTransactionService extends DomainService<PaymentTransaction, PaymentTransactionRepository> {

    @Override
    public Collection<PaymentTransaction> getAllDomains() {
        return getRepository().findDomains();
    }

    @Override
    public void saveDomain(PaymentTransaction transaction) {
        try {
            validateDomain(transaction);
        } catch (DomainNotCreatedException e) {
            throw new RuntimeException("Failed to create payment transaction", e);
        }
        getRepository().save(transaction);
    }

    /**
     * Find payment transaction by AppyPay transaction ID.
     *
     * @param appypayTransactionId the AppyPay transaction ID
     * @return Optional containing the transaction if found
     */
    public java.util.Optional<PaymentTransaction> findByAppyPayTransactionId(String appypayTransactionId) {
        return getRepository().findByAppyPayTransactionId(appypayTransactionId);
    }

    /**
     * Find all transactions for a specific order.
     *
     * @param orderId the order ID
     * @return list of transactions for the order
     */
    public List<PaymentTransaction> findByOrderId(UUID orderId) {
        return getRepository().findByOrderId(orderId);
    }
}
