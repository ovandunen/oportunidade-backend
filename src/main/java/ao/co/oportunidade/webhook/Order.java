package ao.co.oportunidade.webhook;

import ao.co.oportunidade.Domain;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain model representing an order.
 * This is the business logic layer representation, separate from persistence.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends Domain {

    private UUID id;
    private String merchantTransactionId;
    private UUID referenceId;
    private BigDecimal amount;
    private String currency;
    private OrderStatus status;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private Instant createdDate;
    private Instant updatedDate;

    @Override
    public UUID getId() {
        return id;
    }

    /**
     * Order status enumeration
     */
    public enum OrderStatus {
        PENDING,
        PAID,
        FAILED,
        CANCELLED,
        REFUNDED
    }
}
