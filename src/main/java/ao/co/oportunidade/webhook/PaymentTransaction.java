package ao.co.oportunidade.webhook;

import ao.co.oportunidade.Domain;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain model representing a payment transaction.
 * Provides audit trail for all payment activities.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction extends Domain {

    private UUID id;
    private UUID orderId;
    private String appypayTransactionId;
    private BigDecimal amount;
    private String currency;
    private TransactionStatus status;
    private String paymentMethod;
    private String referenceNumber;
    private String referenceEntity;
    private Instant transactionDate;
    private Instant createdDate;
    private Instant updatedDate;
    private String errorMessage;
    private Integer paymentId;

    @Override
    public UUID getId() {
        return id;
    }

    /**
     * Transaction status enumeration
     */
    public enum TransactionStatus {
        SUCCESS,
        PENDING,
        FAILED,
        CANCELLED,
        REFUNDED
    }
}
