package ao.co.oportunidade.order.entity;

import solutions.envision.entity.DomainEntity;
import ao.co.oportunidade.order.model.PackageType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * JPA Entity for Order persistence.
 * Separated from domain model following DDD principles.
 */
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_merchant_tx_id", columnList = "merchantTransactionId"),
    @Index(name = "idx_order_status", columnList = "status"),
    @Index(name = "idx_order_reference_id", columnList = "reference_id"),
    @Index(name = "idx_orders_employer", columnList = "employer_id"),
    @Index(name = "idx_orders_payment", columnList = "merchantTransactionId"),
    @Index(name = "idx_orders_reference_code", columnList = "reference_code")
})
@NamedQueries({
    @NamedQuery(
        name = OrderEntity.FIND_ALL,
        query = "SELECT o FROM OrderEntity o"
    ),
    @NamedQuery(
        name = OrderEntity.FIND_BY_ID,
        query = "SELECT o FROM OrderEntity o WHERE o.id = :id"
    ),
    @NamedQuery(
        name = OrderEntity.FIND_BY_MERCHANT_TX_ID,
        query = "SELECT o FROM OrderEntity o WHERE o.merchantTransactionId = :merchantTxId"
    )
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEntity extends DomainEntity {

    public static final String FIND_ALL = "Order.findAll";
    public static final String FIND_BY_ID = "Order.findById";
    public static final String FIND_BY_MERCHANT_TX_ID = "Order.findByMerchantTxId";
    public static final String PRIMARY_KEY = "id";

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "merchantTransactionId", nullable = false, unique = true, length = 100)
    private String merchantTransactionId;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

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
    
    // ============ NEW FIELDS (Phase 1) ============
    
    /**
     * Internal employer ID (looked up from EmployerReference).
     */
    @Column(name = "employer_id", length = 255)
    private String employerId;
    
    /**
     * Employer's email for notifications.
     */
    @Column(name = "employer_email", length = 255)
    private String employerEmail;
    
    /**
     * Package type purchased (determines token expiration).
     */
    @Column(name = "package_type", length = 50)
    @Enumerated(EnumType.STRING)
    private PackageType packageType;
    
    /**
     * Payment reference code from AppyPay webhook.
     */
    @Column(name = "reference_code", length = 255)
    private String referenceCode;
    
    /**
     * List of specific candidate IDs this order grants access to.
     * Stored in separate table order_candidates.
     */
    @ElementCollection
    @CollectionTable(name = "order_candidates", joinColumns = @JoinColumn(name = "order_id"))
    @Column(name = "candidate_id", length = 255)
    private List<String> candidateIds;
    
    /**
     * List of Odoo document IDs (ir.attachment) to unlock.
     * Stored in separate table order_odoo_documents.
     */
    @ElementCollection
    @CollectionTable(name = "order_odoo_documents", joinColumns = @JoinColumn(name = "order_id"))
    @Column(name = "odoo_document_id", length = 255)
    private List<String> odooDocumentIds;

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

    @Override
    public String toString() {
        return "OrderEntity{" +
                "id=" + id +
                '}';
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDate = Instant.now();
    }
}
