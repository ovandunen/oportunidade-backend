package ao.co.oportunidade.odoo.service;


import ao.co.oportunidade.odoo.dto.OdooPaymentDtoMapper;
import ao.co.oportunidade.odoo.dto.OdooPaymentRequest;
import ao.co.oportunidade.odoo.dto.OdooWebhookResponse;
import ao.co.oportunidade.webhook.PaymentTransaction;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

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
        LOG.infof("Sending payment %s to Odoo webhook", transaction.getId().toString());

        try {
            // Build Odoo request
            final OdooPaymentRequest request = mapOdooRequest(transaction);

            Log.infof("Odoo request payload:\n%s", request);

            // Send to Odoo webhook
            final OdooWebhookResponse response = odooClient.sendPayment(
                    odooWebhookKey,
                    request.getPayment()
            );

            // Check response
            if (Boolean.TRUE.equals(response.getSuccess())) {
                LOG.infof("Successfully sent payment %s to Odoo. Odoo Payment ID: %s",
                        transaction.getId(), response.getPaymentId());

                // Update transaction with Odoo payment ID
                transaction.setPaymentId(response.getPaymentId());

            } else {
                LOG.errorf("Odoo rejected payment %s: %s",
                        transaction.getId().toString(), response.getError());
                throw new OdooSyncException("Odoo rejected payment: " + response.getError());
            }

        } catch (Exception e) {
            LOG.errorf(e, "Failed to send payment to Odoo " + transaction.getId().toString());
            throw new OdooSyncException("Failed to sync to Odoo", e);
        }
    }

    /**
     * Build Odoo payment request from transaction
     */
    private OdooPaymentRequest mapOdooRequest(PaymentTransaction transaction) {

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