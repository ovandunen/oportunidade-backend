package ao.co.oportunidade.payment.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import ao.co.oportunidade.order.model.PackageType;

/**
 * Service for sending notifications to employers and admins.
 * 
 * Phase 1 Implementation: Stub with logging only
 * Phase 2 Implementation: Full email integration with SMTP
 * 
 * Notification Types:
 * - Document access emails (sent to employers after payment)
 * - Payment failure alerts (sent to admins)
 * - Token expiration reminders
 * 
 * @author Oportunidade Team
 * @since Phase 1 - Critical Fixes
 */
@ApplicationScoped
public class NotificationService {
    
    private static final Logger LOG = Logger.getLogger(NotificationService.class);
    
    /**
     * Send document access email to employer with download link.
     * 
     * Phase 1: Logs email content (stub implementation)
     * Phase 2: Integrate with SMTP to actually send emails
     * 
     * Email Template Includes:
     * - Greeting with company name
     * - Number of candidate documents available
     * - Secure download link with JWT token
     * - Token expiration notice
     * - Support contact information
     * 
     * @param employerEmail Recipient email address
     * @param downloadUrl Secure URL with JWT token
     * @param packageType Package type purchased
     * @param candidateCount Number of candidates accessible
     */
    public void sendDocumentAccessEmail(
            String employerEmail,
            String downloadUrl,
            PackageType packageType,
            int candidateCount) {
        
        LOG.infof("===== DOCUMENT ACCESS EMAIL =====");
        LOG.infof("TO: %s", employerEmail);
        LOG.infof("SUBJECT: Your %s Package - %d Candidate Documents Ready", 
            packageType.name(), candidateCount);
        LOG.infof("");
        LOG.infof("BODY:");
        LOG.infof("Dear Employer,");
        LOG.infof("");
        LOG.infof("Thank you for your purchase! Your %s package is now active.", 
            packageType.name());
        LOG.infof("");
        LOG.infof("You now have access to %d candidate document(s).", candidateCount);
        LOG.infof("");
        LOG.infof("Download Link (secure, multi-use):");
        LOG.infof("%s", downloadUrl);
        LOG.infof("");
        LOG.infof("Important Information:");
        LOG.infof("- This link is valid for %d hours", packageType.getExpirationHours());
        LOG.infof("- You can download documents multiple times");
        LOG.infof("- All downloads are logged for security");
        LOG.infof("- Do not share this link with others");
        LOG.infof("");
        LOG.infof("Need help? Contact support@oportunidade.ao");
        LOG.infof("");
        LOG.infof("Best regards,");
        LOG.infof("Oportunidade Team");
        LOG.infof("=================================");
        
        // TODO Phase 2: Implement actual email sending
        // Example:
        // emailClient.send()
        //     .to(employerEmail)
        //     .subject(subject)
        //     .body(htmlBody)
        //     .send();
    }
    
    /**
     * Send payment failure notification to employer.
     * 
     * @param employerEmail Employer email
     * @param orderId Order ID that failed
     * @param errorMessage Error description
     */
    public void sendPaymentFailureNotification(
            String employerEmail,
            String orderId,
            String errorMessage) {
        
        LOG.warnf("===== PAYMENT FAILURE NOTIFICATION =====");
        LOG.warnf("TO: %s", employerEmail);
        LOG.warnf("SUBJECT: Payment Processing Issue - Order %s", orderId);
        LOG.warnf("ERROR: %s", errorMessage);
        LOG.warnf("=========================================");
        
        // TODO Phase 2: Implement actual email sending
    }
    
    /**
     * Send token expiration reminder to employer.
     * 
     * @param employerEmail Employer email
     * @param hoursRemaining Hours until token expires
     * @param downloadUrl Download link (still valid)
     */
    public void sendExpirationReminder(
            String employerEmail,
            int hoursRemaining,
            String downloadUrl) {
        
        LOG.infof("===== TOKEN EXPIRATION REMINDER =====");
        LOG.infof("TO: %s", employerEmail);
        LOG.infof("SUBJECT: Reminder - Your Document Access Expires in %d Hours", hoursRemaining);
        LOG.infof("DOWNLOAD: %s", downloadUrl);
        LOG.infof("======================================");
        
        // TODO Phase 2: Implement actual email sending
    }
}
