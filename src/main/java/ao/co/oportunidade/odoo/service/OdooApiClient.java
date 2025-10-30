package ao.co.oportunidade.odoo.service;


import ao.co.oportunidade.odoo.dto.OdooPaymentRequest;
import ao.co.oportunidade.odoo.dto.OdooWebhookResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "odoo-api")
@Path("/api")
public interface OdooApiClient {

    /**
     * Send payment to Odoo's webhook/API endpoint
     */
    @POST
    @Path("/webhook/payment")  // Adjust to your actual Odoo webhook path
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    OdooWebhookResponse sendPayment(
            @HeaderParam("X-Odoo-Webhook-Key") String webhookKey,
            OdooPaymentRequest.PaymentData payment
    );
}