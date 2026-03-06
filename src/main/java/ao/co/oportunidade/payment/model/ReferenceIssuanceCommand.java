package ao.co.oportunidade.payment.model;


import io.quarkus.narayana.jta.runtime.TransactionScopedNotifier;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@RequiredArgsConstructor
public class ReferenceIssuanceCommand {

    private final TransactionScopedNotifier.TransactionId merchantTransactionId;
    private final BigDecimal amount;
    private final PaymentDescription description;
    private final String currency;
    private final Instant expiresAt;
}
