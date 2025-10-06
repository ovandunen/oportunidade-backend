package ao.co.oportunidade.webhook.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a payment transaction.
 * Provides audit trail for all payment activities.
 */
@Entity
@Table(name = "payment_transactions", indexes = {
    @Index(name = "idx_payment_tx_appypay_id", columnList = "appypayTransactionId"),
    @Index(name = "idx_payment_tx_order_id", columnList = "order_id"),
    @Index(name = "idx_payment_tx_status", columnList = "status"),
    @Index(name = "idx_payment_tx_created", columnList = "transactionDate")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction extends PanacheEntityBase {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "appypayTransactionId", nullable = false, length = 100)
    private String appypayTransactionId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "reference_number", length = 50)
    private String referenceNumber;

    @Column(name = "reference_entity", length = 20)
    private String referenceEntity;

    @Column(name = "transaction_date", nullable = false)
    private Instant transactionDate;

    @Column(name = "created_date", nullable = false)
    private Instant createdDate;

    @Column(name = "updated_date", nullable = false)
    private Instant updatedDate;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        if (createdDate == null) {
            createdDate = now;
        }
        updatedDate = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDate = Instant.now();
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
