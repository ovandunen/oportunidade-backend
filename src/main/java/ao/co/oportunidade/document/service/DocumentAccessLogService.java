package ao.co.oportunidade.document.service;

import ao.co.oportunidade.document.entity.DocumentAccessAuditLog;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing document access logs.
 * Handles creation and querying of access audit records.
 */
@ApplicationScoped
public class DocumentAccessLogService {

    private static final Logger LOG = Logger.getLogger(DocumentAccessLogService.class);

    /**
     * Log a successful document access.
     * 
     * @param orderId the order ID
     * @param employerId the employer ID
     * @param employerEmail the employer email
     * @param tokenUsed the token used for access
     * @param tokenType the token type
     * @param candidateId the candidate ID
     * @param documentId the document ID
     * @param documentName the document name
     * @param ipAddress the IP address
     * @param userAgent the user agent
     * @param documentSizeBytes the document size in bytes
     * @return the created access log
     */
    @Transactional
    public DocumentAccessAuditLog logSuccessfulAccess(
            UUID orderId,
            String employerId,
            String employerEmail,
            String tokenUsed,
            DocumentAccessAuditLog.TokenType tokenType,
            String candidateId,
            String documentId,
            String documentName,
            String ipAddress,
            String userAgent,
            Long documentSizeBytes) {

        DocumentAccessAuditLog log = DocumentAccessAuditLog.builder()
                .orderId(orderId)
                .employerId(employerId)
                .employerEmail(employerEmail)
                .tokenUsed(truncateToken(tokenUsed))
                .tokenType(tokenType)
                .candidateId(candidateId)
                .documentId(documentId)
                .documentName(documentName)
                .ipAddress(ipAddress)
                .userAgent(truncateUserAgent(userAgent))
                .accessedAt(Instant.now())
                .success(true)
                .httpStatusCode(200)
                .documentSizeBytes(documentSizeBytes)
                .build();

        log.persist();

        LOG.infof("Logged successful access: order=%s, employer=%s, document=%s, ip=%s",
                orderId, employerId, documentId, ipAddress);

        return log;
    }

    /**
     * Log a failed document access attempt.
     * 
     * @param orderId the order ID
     * @param employerId the employer ID
     * @param employerEmail the employer email
     * @param tokenUsed the token used for access
     * @param tokenType the token type
     * @param candidateId the candidate ID
     * @param documentId the document ID
     * @param ipAddress the IP address
     * @param userAgent the user agent
     * @param httpStatusCode the HTTP status code
     * @param errorMessage the error message
     * @return the created access log
     */
    @Transactional
    public DocumentAccessAuditLog logFailedAccess(
            UUID orderId,
            String employerId,
            String employerEmail,
            String tokenUsed,
            DocumentAccessAuditLog.TokenType tokenType,
            String candidateId,
            String documentId,
            String ipAddress,
            String userAgent,
            Integer httpStatusCode,
            String errorMessage) {

        DocumentAccessAuditLog log = DocumentAccessAuditLog.builder()
                .orderId(orderId)
                .employerId(employerId)
                .employerEmail(employerEmail)
                .tokenUsed(truncateToken(tokenUsed))
                .tokenType(tokenType)
                .candidateId(candidateId)
                .documentId(documentId)
                .ipAddress(ipAddress)
                .userAgent(truncateUserAgent(userAgent))
                .accessedAt(Instant.now())
                .success(false)
                .httpStatusCode(httpStatusCode)
                .errorMessage(truncateErrorMessage(errorMessage))
                .build();

        log.persist();

        LOG.warnf("Logged failed access: order=%s, employer=%s, document=%s, ip=%s, error=%s",
                orderId, employerId, documentId, ipAddress, errorMessage);

        return log;
    }

    /**
     * Get access logs for a specific order.
     * 
     * @param orderId the order ID
     * @return list of access logs
     */
    public List<DocumentAccessAuditLog> getAccessLogsForOrder(UUID orderId) {
        return DocumentAccessAuditLog.findByOrderId(orderId);
    }

    /**
     * Get access logs for a specific employer.
     * 
     * @param employerId the employer ID
     * @return list of access logs
     */
    public List<DocumentAccessAuditLog> getAccessLogsForEmployer(String employerId) {
        return DocumentAccessAuditLog.findByEmployerId(employerId);
    }

    /**
     * Get access logs using a specific token.
     * 
     * @param token the token
     * @return list of access logs
     */
    public List<DocumentAccessAuditLog> getAccessLogsForToken(String token) {
        return DocumentAccessAuditLog.findByToken(truncateToken(token));
    }

    /**
     * Count successful downloads for an order.
     * 
     * @param orderId the order ID
     * @return count of successful downloads
     */
    public long countSuccessfulDownloads(UUID orderId) {
        return DocumentAccessAuditLog.countSuccessfulByOrderId(orderId);
    }

    /**
     * Get recent access logs (last N hours).
     * 
     * @param hours number of hours to look back
     * @return list of recent access logs
     */
    public List<DocumentAccessAuditLog> getRecentAccessLogs(int hours) {
        return DocumentAccessAuditLog.findRecent(hours);
    }

    /**
     * Get failed access attempts.
     * Useful for security monitoring.
     * 
     * @return list of failed access logs
     */
    public List<DocumentAccessAuditLog> getFailedAccessAttempts() {
        return DocumentAccessAuditLog.findFailedAccess();
    }

    /**
     * Get access logs from a specific IP address.
     * Useful for detecting suspicious activity.
     * 
     * @param ipAddress the IP address
     * @return list of access logs
     */
    public List<DocumentAccessAuditLog> getAccessLogsByIp(String ipAddress) {
        return DocumentAccessAuditLog.findByIpAddress(ipAddress);
    }

    /**
     * Truncate JWT token for storage (keep only last 50 chars for reference).
     * Full tokens are too long to store efficiently.
     * 
     * @param token the token
     * @return truncated token
     */
    private String truncateToken(String token) {
        if (token == null) {
            return null;
        }
        if (token.length() <= 100) {
            return token;
        }
        // Keep first 50 and last 50 characters
        return token.substring(0, 50) + "..." + token.substring(token.length() - 47);
    }

    /**
     * Truncate user agent string to fit database column.
     * 
     * @param userAgent the user agent
     * @return truncated user agent
     */
    private String truncateUserAgent(String userAgent) {
        if (userAgent == null) {
            return null;
        }
        return userAgent.length() <= 1000 ? userAgent : userAgent.substring(0, 997) + "...";
    }

    /**
     * Truncate error message to fit database column.
     * 
     * @param errorMessage the error message
     * @return truncated error message
     */
    private String truncateErrorMessage(String errorMessage) {
        if (errorMessage == null) {
            return null;
        }
        return errorMessage.length() <= 1000 ? errorMessage : errorMessage.substring(0, 997) + "...";
    }
}
