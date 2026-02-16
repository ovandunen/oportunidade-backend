package ao.co.oportunidade.employer.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;

/**
 * Maps payment reference codes to employer identities.
 * This enables lookup of employer information from AppyPay payment webhooks.
 * 
 * @author Oportunidade Team
 * @since Phase 1 - Critical Fixes
 */
@Entity
@Table(name = "employer_references", indexes = {
    @Index(name = "idx_employer_ref_code", columnList = "reference_code"),
    @Index(name = "idx_employer_id", columnList = "employer_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployerReference extends PanacheEntity {
    
    /**
     * Payment reference code from AppyPay webhook.
     * This is the unique identifier sent in the payment notification.
     */
    @Column(name = "reference_code", unique = true, nullable = false, length = 255)
    private String referenceCode;
    
    /**
     * Internal employer ID (our system's identifier).
     */
    @Column(name = "employer_id", nullable = false, length = 255)
    private String employerId;
    
    /**
     * Employer's email address for notifications.
     */
    @Column(name = "employer_email", length = 255)
    private String employerEmail;
    
    /**
     * Company name for display purposes.
     */
    @Column(name = "company_name", length = 500)
    private String companyName;
    
    /**
     * Timestamp when this reference was created.
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    /**
     * Whether this reference is currently active.
     * Inactive references cannot be used for new payments.
     */
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
    
    // ============ Panache Finder Methods ============
    
    /**
     * Find employer reference by payment reference code.
     * 
     * @param code The reference code from payment webhook
     * @return EmployerReference or null if not found
     */
    public static EmployerReference findByReferenceCode(String code) {
        return find("referenceCode = ?1 and isActive = true", code).firstResult();
    }
    
    /**
     * Find employer reference by internal employer ID.
     * 
     * @param employerId Our internal employer identifier
     * @return EmployerReference or null if not found
     */
    public static EmployerReference findByEmployerId(String employerId) {
        return find("employerId = ?1 and isActive = true", employerId).firstResult();
    }
}
