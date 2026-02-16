package ao.co.oportunidade.order.model;

import solutions.envision.model.Domain;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Domain model representing an order.
 * This is the business logic layer representation, separate from persistence.
 * 
 * Enhanced in Phase 1 to support payment-to-document mapping:
 * - Links orders to specific employers
 * - Associates orders with candidate IDs
 * - Tracks package type for token generation
 * - Maps to Odoo document IDs for access control
 * 
 * @author Oportunidade Team
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
    
    // ============ NEW FIELDS (Phase 1) ============
    
    /**
     * Internal employer ID (looked up from EmployerReference).
     * Used to identify which employer made the payment.
     */
    private String employerId;
    
    /**
     * Employer's email for notifications and audit trail.
     */
    private String employerEmail;
    
    /**
     * Package type purchased (determines token expiration).
     */
    private PackageType packageType;
    
    /**
     * List of specific candidate IDs this order grants access to.
     * Each candidate ID corresponds to an hr.applicant record in Odoo.
     */
    private List<String> candidateIds;
    
    /**
     * List of Odoo document IDs (ir.attachment) to unlock.
     * Mapped from candidateIds via Odoo API or internal mapping.
     */
    private List<String> odooDocumentIds;
    
    /**
     * Payment reference code from AppyPay webhook.
     * Used to lookup EmployerReference.
     */
    private String referenceCode;

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
        REFUNDED,
        COMPLETED  // Successfully processed and token generated
    }
}
