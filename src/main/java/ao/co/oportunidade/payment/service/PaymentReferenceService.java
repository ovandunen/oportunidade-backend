package ao.co.oportunidade.payment.service;

import ao.co.oportunidade.payment.model.IssuedReference;
import ao.co.oportunidade.payment.model.PaymentReference;
import ao.co.oportunidade.payment.model.ReferenceIssuanceCommand;
import ao.co.oportunidade.payment.repository.PaymentReferenceRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static ao.co.oportunidade.payment.model.PaymentReference.*;

@ApplicationScoped
public class PaymentReferenceService {

    private final PaymentReferenceRepository referenceRepository;
    private final ReferenceIssuancePort referenceIssuancePort;

    @Inject
    public PaymentReferenceService(
            PaymentReferenceRepository referenceRepository,
            ReferenceIssuancePort referenceIssuancePort) {
        this.referenceRepository = referenceRepository;
        this.referenceIssuancePort = referenceIssuancePort;
    }

    @Transactional
    public PaymentReference issueReference(ReferenceIssuanceCommand command) {

        validateAmount(command.getAmount());
        // Domain delegates intent — knows nothing about how it is fulfilled
        IssuedReference issued = referenceIssuancePort.issue(command);

        final PaymentReference reference
         = new PaymentReference().issue(
                issued.getReferenceNumber(),
                command.getMerchantTransactionId().toString(),
                issued.getEntityCode(),
                command.getAmount(),
                command.getCurrency(),
                command.getExpiresAt());

        referenceRepository.save(reference);
        return reference;
    }

    private void validateAmount(BigDecimal amount) {
        if (amount.longValue()<=0) {
            throw new IllegalArgumentException("Amount deve ser positiva!");
        }
    }
}