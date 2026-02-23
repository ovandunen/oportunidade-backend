package ao.co.oportunidade.notification.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Service for sending admin alerts via Slack.
 * Used for critical operational issues that require immediate attention.
 * 
 * Phase 1: Logs alerts to console
 * Phase 2: Sends actual Slack webhooks
 */
@ApplicationScoped
public class AlertService {

    private static final Logger LOG = Logger.getLogger(AlertService.class);

    @ConfigProperty(name = "slack.webhook-url")
    String slackWebhookUrl;

    @ConfigProperty(name = "slack.channel", defaultValue = "#alerts")
    String slackChannel;

    @ConfigProperty(name = "slack.enabled", defaultValue = "false")
    boolean slackEnabled;

    @ConfigProperty(name = "app.environment", defaultValue = "dev")
    String environment;

    /**
     * Send critical alert when webhook processing fails after all retries.
     * 
     * @param transactionId the transaction ID that failed
     * @param errorMessage the error message
     * @param attemptCount number of attempts made
     */
    public void sendWebhookFailureAlert(String transactionId, String errorMessage, int attemptCount) {
        String message = String.format(
            "🚨 *CRITICAL: Webhook Processing Failed*\n" +
            "Environment: `%s`\n" +
            "Transaction ID: `%s`\n" +
            "Attempts: %d\n" +
            "Error: ```%s```\n" +
            "Action Required: Manual investigation needed",
            environment,
            transactionId,
            attemptCount,
            truncate(errorMessage, 500)
        );

        sendAlert("webhook_failure", message, AlertPriority.CRITICAL);
    }

    /**
     * Send alert when payment processing fails.
     * 
     * @param orderId the order ID
     * @param transactionId the transaction ID
     * @param errorMessage the error message
     */
    public void sendPaymentProcessingAlert(UUID orderId, String transactionId, String errorMessage) {
        String message = String.format(
            "⚠️ *Payment Processing Error*\n" +
            "Environment: `%s`\n" +
            "Order ID: `%s`\n" +
            "Transaction ID: `%s`\n" +
            "Error: ```%s```\n" +
            "Status: Retrying automatically",
            environment,
            orderId,
            transactionId,
            truncate(errorMessage, 500)
        );

        sendAlert("payment_processing", message, AlertPriority.HIGH);
    }

    /**
     * Send alert when Odoo API calls fail.
     * 
     * @param operation the operation that failed (e.g., "fetchDocument", "authenticate")
     * @param errorMessage the error message
     */
    public void sendOdooApiFailureAlert(String operation, String errorMessage) {
        String message = String.format(
            "⚠️ *Odoo API Failure*\n" +
            "Environment: `%s`\n" +
            "Operation: `%s`\n" +
            "Error: ```%s```\n" +
            "Status: Retrying automatically",
            environment,
            operation,
            truncate(errorMessage, 500)
        );

        sendAlert("odoo_api_failure", message, AlertPriority.HIGH);
    }

    /**
     * Send alert when token generation fails.
     * 
     * @param orderId the order ID
     * @param errorMessage the error message
     */
    public void sendTokenGenerationAlert(UUID orderId, String errorMessage) {
        String message = String.format(
            "🔴 *Token Generation Failed*\n" +
            "Environment: `%s`\n" +
            "Order ID: `%s`\n" +
            "Error: ```%s```\n" +
            "Impact: Employer cannot access documents",
            environment,
            orderId,
            truncate(errorMessage, 500)
        );

        sendAlert("token_generation", message, AlertPriority.CRITICAL);
    }

    /**
     * Send alert when employer reference is not found.
     * 
     * @param referenceCode the reference code that was not found
     * @param transactionId the transaction ID
     */
    public void sendEmployerReferenceNotFoundAlert(String referenceCode, String transactionId) {
        String message = String.format(
            "⚠️ *Employer Reference Not Found*\n" +
            "Environment: `%s`\n" +
            "Reference Code: `%s`\n" +
            "Transaction ID: `%s`\n" +
            "Impact: Cannot map payment to employer\n" +
            "Action: Verify reference code or create employer record",
            environment,
            referenceCode,
            transactionId
        );

        sendAlert("employer_reference_missing", message, AlertPriority.HIGH);
    }

    /**
     * Send alert when database transaction fails.
     * 
     * @param operation the database operation
     * @param errorMessage the error message
     */
    public void sendDatabaseFailureAlert(String operation, String errorMessage) {
        String message = String.format(
            "🔴 *Database Operation Failed*\n" +
            "Environment: `%s`\n" +
            "Operation: `%s`\n" +
            "Error: ```%s```\n" +
            "Impact: Data consistency may be affected",
            environment,
            operation,
            truncate(errorMessage, 500)
        );

        sendAlert("database_failure", message, AlertPriority.CRITICAL);
    }

    /**
     * Send a generic alert.
     * 
     * @param alertType the type of alert
     * @param message the alert message
     * @param priority the alert priority
     */
    private void sendAlert(String alertType, String message, AlertPriority priority) {
        if (slackEnabled) {
            try {
                sendToSlack(message, priority);
            } catch (Exception e) {
                LOG.errorf(e, "Failed to send Slack alert: %s", alertType);
                // Fall back to logging
                logAlert(alertType, message, priority);
            }
        } else {
            // Phase 1: Just log the alert
            logAlert(alertType, message, priority);
        }
    }

    /**
     * Send alert to Slack webhook (Phase 2 implementation).
     * 
     * @param message the message to send
     * @param priority the alert priority
     */
    private void sendToSlack(String message, AlertPriority priority) {
        // TODO Phase 2: Implement actual Slack webhook integration
        // For now, use REST client to POST to Slack webhook URL
        
        // Slack message format:
        // {
        //   "channel": "#alerts",
        //   "username": "Oportunidade Alert Bot",
        //   "icon_emoji": priority == CRITICAL ? ":rotating_light:" : ":warning:",
        //   "text": message
        // }
        
        LOG.infof("Would send to Slack: %s", message);
    }

    /**
     * Log alert to console (Phase 1 implementation).
     * 
     * @param alertType the alert type
     * @param message the message
     * @param priority the priority
     */
    private void logAlert(String alertType, String message, AlertPriority priority) {
        String logMessage = String.format(
            "\n" +
            "================== ALERT ==================\n" +
            "Type: %s\n" +
            "Priority: %s\n" +
            "Timestamp: %s\n" +
            "%s\n" +
            "==========================================\n",
            alertType,
            priority,
            Instant.now(),
            message
        );

        if (priority == AlertPriority.CRITICAL) {
            LOG.error(logMessage);
        } else {
            LOG.warn(logMessage);
        }
    }

    /**
     * Truncate long error messages for readability.
     * 
     * @param text the text to truncate
     * @param maxLength maximum length
     * @return truncated text
     */
    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "null";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "... (truncated)";
    }

    /**
     * Alert priority levels.
     */
    public enum AlertPriority {
        HIGH,      // Warning - automatic retry in progress
        CRITICAL   // Requires immediate manual intervention
    }
}
