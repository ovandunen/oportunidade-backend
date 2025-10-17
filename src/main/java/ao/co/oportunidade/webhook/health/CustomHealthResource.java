package ao.co.oportunidade.webhook.health;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.Map;

import static ao.co.oportunidade.Resource.SERVER_FAILURE;
import static javax.security.auth.callback.ConfirmationCallback.OK;

@Path("/api/health")
@Produces(MediaType.APPLICATION_JSON)
public class CustomHealthResource  {

    @Inject
    MicrometerHealthService healthService;

    @GET
    @Path("/status")
    public Response getHealthStatus() {
        final boolean dbHealth = healthService.checkDatabaseHealth();
        final boolean webhookHealth = healthService.checkWebhookServiceHealth();

        final Map<String, Object> status = new HashMap<>();
        status.put("database", dbHealth ? "UP" : "DOWN");
        status.put("webhook_service", webhookHealth ? "UP" : "DOWN");
        status.put("overall", (dbHealth && webhookHealth) ? "UP" : "DOWN");

        final int httpStatus = (dbHealth && webhookHealth) ? OK : SERVER_FAILURE;

        return Response.status(httpStatus).entity(status).build();
    }
}
