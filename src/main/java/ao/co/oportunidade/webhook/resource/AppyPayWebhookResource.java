package ao.co.oportunidade.webhook.resource;

import ao.co.oportunidade.webhook.WebhookEvent;
import ao.co.oportunidade.webhook.dto.AppyPayWebhookPayload;
import ao.co.oportunidade.webhook.dto.WebhookResponse;
import ao.co.oportunidade.webhook.service.WebhookEventServiceFacade;
import io.smallrye.reactive.messaging.annotations.Channel;
import io.smallrye.reactive.messaging.annotations.Emitter;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

/**
 * REST Resource for AppyPay webhook endpoint.
 * Receives payment notifications from AppyPay and processes them asynchronously.
 * Refactored to use DDD service facade.
 */
@Path("/webhooks/appypay")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AppyPayWebhookResource {

    private static final Logger LOG = Logger.getLogger(AppyPayWebhookResource.class);

    @Inject
    WebhookEventServiceFacade webhookEventService;

    @Inject
    @Channel("webhook-events-out")
    Emitter<AppyPayWebhookPayload> webhookEmitter;

    /**
     * Webhook endpoint for receiving AppyPay payment notifications.
     * Responds immediately with 200 OK and processes asynchronously.
     *
     * @param payload the webhook payload from AppyPay
     * @return webhook response
     */
    @POST
    public Response receiveWebhook(AppyPayWebhookPayload payload) {
        final String transactionId = payload.getId();
        final String merchantTxId = payload.getMerchantTransactionId();

        LOG.infof("Received webhook for transaction: %s, merchant: %s, status: %s",
                transactionId, merchantTxId, payload.getStatus());

        try {
            // Idempotency check
            if (webhookEventService.isAlreadyProcessed(transactionId)) {
                LOG.infof("Webhook already processed (idempotent): %s", transactionId);
                return Response.ok(WebhookResponse.builder()
                        .status("already_processed")
                        .message("Webhook already processed")
                        .eventId(transactionId)
                        .build())
                        .build();
            }

            // Create webhook event record
            final WebhookEvent event = webhookEventService.createWebhookEvent(payload);

            // Send to async processing queue
            webhookEmitter.send(payload);

            LOG.infof("Webhook queued for processing: %s", transactionId);

            // Return immediate response (within 30 seconds as required)
            return Response.ok(WebhookResponse.success(event.getId().toString())).build();

        } catch (Exception e) {
            LOG.errorf(e, "Error receiving webhook for transaction: %s", transactionId);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(WebhookResponse.error("Failed to process webhook: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Health check endpoint for webhook service.
     *
     * @return health status
     */
    @GET
    @Path("/health")
    public Response health() {
        return Response.ok()
                .entity("{\"status\": \"UP\", \"service\": \"AppyPay Webhook\"}")
                .build();
    }
}
