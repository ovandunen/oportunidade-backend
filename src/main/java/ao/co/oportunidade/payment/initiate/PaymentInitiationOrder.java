package ao.co.oportunidade.payment.initiate;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "payment_initiation_orders")
public class PaymentInitiationOrder extends PanacheEntityBase {

    @Id
    @Column(name = "id", nullable = false)
    public UUID id;

    @Column(name = "user_id", nullable = false, length = 255)
    public String userId;

    @Column(name = "resource_id", nullable = false, length = 255)
    public String resourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 64)
    public PaymentResourceType resourceType;

    @Column(name = "merchant_transaction_id", nullable = false, unique = true, length = 100)
    public String merchantTransactionId;

    @Column(name = "payment_url", nullable = false, columnDefinition = "TEXT")
    public String paymentUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    public OrderStatus status;

    @Column(name = "payment_session_expires_at", nullable = false)
    public Instant paymentSessionExpiresAt;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    public static Optional<PaymentInitiationOrder> findByUserAndResource(String userId, String resourceId) {
        return find("userId = ?1 and resourceId = ?2", userId, resourceId).firstResultOptional();
    }

    public static boolean existsPaidForResourceDifferentUser(String resourceId, String userId) {
        return count("resourceId = ?1 and status = ?2 and userId <> ?3",
                resourceId, OrderStatus.PAID, userId) > 0;
    }
}
