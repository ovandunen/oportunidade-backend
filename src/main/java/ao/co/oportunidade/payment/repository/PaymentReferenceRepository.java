package ao.co.oportunidade.payment.repository;

import ao.co.oportunidade.payment.entity.PaymentReferenceEntity;
import ao.co.oportunidade.payment.entity.PaymentReferenceMapper;
import ao.co.oportunidade.payment.model.PaymentReference;
import io.quarkus.narayana.jta.runtime.TransactionScopedNotifier;
import jakarta.enterprise.context.ApplicationScoped;
import solutions.envision.entity.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;


@ApplicationScoped
public class PaymentReferenceRepository extends Repository<PaymentReference, PaymentReferenceEntity, PaymentReferenceMapper> {


    @Override
    protected Collection<PaymentReference> findDomains() {
        final List<PaymentReferenceEntity> references = getEntityManager()
                .createNamedQuery(PaymentReferenceEntity.FIND_ALL, PaymentReferenceEntity.class)
                .getResultStream()
                .toList();
        return references.stream()
                .map(getMapper()::mapToDomain)
                .toList();
    }

    @Override
    public Optional<PaymentReference> findDomainById(PaymentReference domain) {
        try {
            final PaymentReferenceEntity reference = getEntityManager()
                    .createNamedQuery(PaymentReferenceEntity.FIND_BY_ID, PaymentReferenceEntity.class)
                    .setParameter(PaymentReferenceEntity.PRIMARY_KEY, domain.getId())
                    .getSingleResult();
            return Optional.ofNullable(getMapper().mapToDomain(reference));
        } catch (Exception e) {
            return Optional.empty();
        }
    }


    public Optional<PaymentReference> findByMerchantTransactionId(TransactionScopedNotifier.TransactionId merchantTransactionId) {
        return null;
    }
}
