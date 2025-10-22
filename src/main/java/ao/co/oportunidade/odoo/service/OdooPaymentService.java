package ao.co.oportunidade.odoo.service;


import ao.co.oportunidade.odoo.dto.OdooPaymentDtoMapper;
import ao.co.oportunidade.odoo.dto.OdooPaymentRequest;
import ao.co.oportunidade.odoo.dto.OdooWebhookResponse;
import ao.co.oportunidade.webhook.PaymentTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@ApplicationScoped
public class OdooPaymentService {

    private static final Logger LOG = Logger.getLogger(OdooPaymentService.class);

    @Inject
    @RestClient
    OdooApiClient odooClient;

    @Inject
    OdooPaymentDtoMapper mapper;

    @ConfigProperty(name = "odoo.webhook.key")
    String odooWebhookKey;

    @ConfigProperty(name = "odoo.default.currency-id", defaultValue = "1")
    Integer defaultCurrencyId;

    @ConfigProperty(name = "odoo.default.journal-id", defaultValue = "1")
    Integer defaultJournalId;

    @ConfigProperty(name = "odoo.default.payment-method-id", defaultValue = "1")
    Integer defaultPaymentMethodId;

    /**
     * Send payment to Odoo via their webhook endpoint
     */
    public void sendPaymentToOdoo(PaymentTransaction transaction) {
        LOG.infof("Sending payment %d to Odoo webhook", transaction.getId());

        try {
            // Build Odoo request
            OdooPaymentRequest request = buildOdooRequest(transaction);

            // Send to Odoo webhook
            OdooWebhookResponse response = odooClient.sendPayment(
                    odooWebhookKey,
                    request
            );

            // Check response
            if (response.getSuccess() != null && response.getSuccess()) {
                LOG.infof("Successfully sent payment %d to Odoo. Odoo Payment ID: %d",
                        transaction.getId(), response.getPaymentId());

                // Update transaction with Odoo payment ID
                transaction.setPaymentId(response.getPaymentId());

            } else {
                LOG.errorf("Odoo rejected payment %d: %s",
                        transaction.getId(), response.getError());
                throw new OdooSyncException("Odoo rejected payment: " + response.getError());
            }

        } catch (Exception e) {
            LOG.errorf(e, "Failed to send payment %d to Odoo", transaction.getId());
            throw new OdooSyncException("Failed to sync to Odoo", e);
        }
    }

    /**
     * Build Odoo payment request from transaction
     */
    private OdooPaymentRequest buildOdooRequest(PaymentTransaction transaction) {

            final OdooPaymentRequest.PaymentData paymentData = mapper.mapToDto(transaction);
            return new OdooPaymentRequest(paymentData);
    }
}

// Exception class
class OdooSyncException extends RuntimeException {
    public OdooSyncException(String message) {
        super(message);
    }

    public OdooSyncException(String message, Throwable cause) {
        super(message, cause);
    }
}