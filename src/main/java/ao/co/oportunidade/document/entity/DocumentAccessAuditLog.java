package ao.co.oportunidade.document.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Entity to track document access for audit and compliance purposes.
 * Records every document access attempt (successful and failed).
 * 
 * Purpose:
 * - Audit trail for document downloads
 * - Track token usage patterns
 * - Detect suspicious access patterns
 * - Compliance with data protection regulations
 * - Support for multi-use token tracking
 */
@Entity(name = "DocumentAccessAuditLog")
@Table(name = "document_access_logs", indexes = {
    @Index(name = "idx_access_log_token", columnList = "token_used"),
    @Index(name = "idx_access_log_order", columnList = "order_id"),
    @Index(name = "idx_access_log_employer", columnList = "employer_id"),
    @Index(name = "idx_access_log_timestamp", columnList = "accessed_at"),
    @Index(name = "idx_access_log_ip", columnList = "ip_address"),
    @Index(name = "idx_access_log_success", columnList = "success")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentAccessAuditLog extends PanacheEntityBase {

    /**
     * Primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Order ID associated with this access.
     */
    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    /**
     * Employer ID who accessed the document.
     */
    @Column(name = "employer_id", nullable = false, length = 255)
    private String employerId;

    /**
     * Email of the employer.
     */
    @Column(name = "employer_email", length = 255)
    private String employerEmail;

    /**
     * Token used for access (JWT or single-use token).
     */
    @Column(name = "token_used", nullable = false, length = 2000)
    private String tokenUsed;

    /**
     * Type of token used (MULTI_USE or SINGLE_USE).
     */
    @Column(name = "token_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TokenType tokenType;

    /**
     * Candidate ID accessed.
     */
    @Column(name = "candidate_id", length = 255)
    private String candidateId;

    /**
     * Odoo document ID accessed.
     */
    @Column(name = "document_id", length = 255)
    private String documentId;

    /**
     * Document name/filename.
     */
    @Column(name = "document_name", length = 500)
    private String documentName;

    /**
     * IP address of the accessor.
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * User agent string from the HTTP request.
     */
    @Column(name = "user_agent", length = 1000)
    private String userAgent;

    /**
     * Timestamp of the access attempt.
     */
    @Column(name = "accessed_at", nullable = false)
    private Instant accessedAt;

    /**
     * Whether the access was successful.
     */
    @Column(name = "success", nullable = false)
    private Boolean success;

    /**
     * Error message if access failed.
     */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    /**
     * HTTP status code returned.
     */
    @Column(name = "http_status_code")
    private Integer httpStatusCode;

    /**
     * Size of the document in bytes (if successfully downloaded).
     */
    @Column(name = "document_size_bytes")
    private Long documentSizeBytes;

    /**
     * Token type enumeration.
     */
    public enum TokenType {
        MULTI_USE,   // JWT token that can be used multiple times until expiration
        SINGLE_USE   // Single-use document token
    }

    // ============= Panache Finder Methods =============

    /**
     * Find all access logs for a specific order.
     * 
     * @param orderId the order ID
     * @return list of access logs
     */
    public static List<DocumentAccessAuditLog> findByOrderId(UUID orderId) {
        return list("orderId", orderId);
    }

    /**
     * Find all access logs for a specific employer.
     * 
     * @param employerId the employer ID
     * @return list of access logs
     */
    public static List<DocumentAccessAuditLog> findByEmployerId(String employerId) {
        return list("employerId", employerId);
    }

    /**
     * Find all access logs using a specific token.
     * 
     * @param tokenUsed the token
     * @return list of access logs
     */
    public static List<DocumentAccessAuditLog> findByToken(String tokenUsed) {
        return list("tokenUsed", tokenUsed);
    }

    /**
     * Find all access logs from a specific IP address.
     * Useful for detecting suspicious access patterns.
     * 
     * @param ipAddress the IP address
     * @return list of access logs
     */
    public static List<DocumentAccessAuditLog> findByIpAddress(String ipAddress) {
        return list("ipAddress", ipAddress);
    }

    /**
     * Find failed access attempts.
     * 
     * @return list of failed access logs
     */
    public static List<DocumentAccessAuditLog> findFailedAccess() {
        return list("success", false);
    }

    /**
     * Count access attempts for a specific token.
     * 
     * @param tokenUsed the token
     * @return count of access attempts
     */
    public static long countByToken(String tokenUsed) {
        return count("tokenUsed", tokenUsed);
    }

    /**
     * Count successful downloads for an order.
     * 
     * @param orderId the order ID
     * @return count of successful downloads
     */
    public static long countSuccessfulByOrderId(UUID orderId) {
        return count("orderId = ?1 and success = true", orderId);
    }

    /**
     * Find recent access logs (last N hours).
     * 
     * @param hours number of hours to look back
     * @return list of recent access logs
     */
    public static List<DocumentAccessAuditLog> findRecent(int hours) {
        Instant cutoff = Instant.now().minusSeconds(hours * 3600L);
        return list("accessedAt >= ?1 order by accessedAt desc", cutoff);
    }

    /**
     * Find access logs for a specific candidate.
     * 
     * @param candidateId the candidate ID
     * @return list of access logs
     */
    public static List<DocumentAccessAuditLog> findByCandidateId(String candidateId) {
        return list("candidateId", candidateId);
    }

    /**
     * Find access logs between two timestamps.
     * 
     * @param start start timestamp
     * @param end end timestamp
     * @return list of access logs
     */
    public static List<DocumentAccessAuditLog> findBetween(Instant start, Instant end) {
        return list("accessedAt >= ?1 and accessedAt <= ?2 order by accessedAt desc", start, end);
    }
}
