package ao.co.oportunidade.webhook.entity;

import ao.co.oportunidade.DomainEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity for PaymentTransaction persistence.
 * Separated from domain model following DDD principles.
 */
@Entity
@Table(name = "payment_transactions", indexes = {
    @Index(name = "idx_payment_tx_appypay_id", columnList = "appypayTransactionId"),
    @Index(name = "idx_payment_tx_order_id", columnList = "order_id"),
    @Index(name = "idx_payment_tx_status", columnList = "status"),
    @Index(name = "idx_payment_tx_created", columnList = "transactionDate")
})
@NamedQueries({
    @NamedQuery(
        name = PaymentTransactionEntity.FIND_ALL,
        query = "SELECT pt FROM PaymentTransactionEntity pt"
    ),
    @NamedQuery(
        name = PaymentTransactionEntity.FIND_BY_ID,
        query = "SELECT pt FROM PaymentTransactionEntity pt WHERE pt.id = :id"
    ),
    @NamedQuery(
        name = PaymentTransactionEntity.FIND_BY_APPYPAY_TX_ID,
        query = "SELECT pt FROM PaymentTransactionEntity pt WHERE pt.appypayTransactionId = :appypayTxId"
    )
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransactionEntity extends DomainEntity {

    public static final String FIND_ALL = "PaymentTransaction.findAll";
    public static final String FIND_BY_ID = "PaymentTransaction.findById";
    public static final String FIND_BY_APPYPAY_TX_ID = "PaymentTransaction.findByAppyPayTxId";
    public static final String PRIMARY_KEY = "id";

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

    @Column(name = "status", nullable = false, length = 20)
    private String status;

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
}
