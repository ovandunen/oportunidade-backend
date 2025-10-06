package ao.co.oportunidade.webhook.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing an order that is awaiting or has received payment.
 * Links to Reference entity for AppyPay payment references.
 */
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_merchant_tx_id", columnList = "merchantTransactionId"),
    @Index(name = "idx_order_status", columnList = "status"),
    @Index(name = "idx_order_reference_id", columnList = "reference_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends PanacheEntityBase {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "merchantTransactionId", nullable = false, unique = true, length = 100)
    private String merchantTransactionId;

    @Column(name = "reference_id")
    private UUID referenceId; // Links to Reference entity

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "customer_name", length = 255)
    private String customerName;

    @Column(name = "customer_email", length = 255)
    private String customerEmail;

    @Column(name = "customer_phone", length = 50)
    private String customerPhone;

    @Column(name = "created_date", nullable = false)
    private Instant createdDate;

    @Column(name = "updated_date", nullable = false)
    private Instant updatedDate;

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
     * Order status enumeration
     */
    public enum OrderStatus {
        PENDING,     // Order created, awaiting payment
        PAID,        // Payment received and confirmed
        FAILED,      // Payment failed
        CANCELLED,   // Order cancelled
        REFUNDED     // Payment refunded
    }
}
