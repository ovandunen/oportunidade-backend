package ao.co.oportunidade.webhook.resource;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for webhook-related exceptions.
 */
@Provider
public class WebhookExceptionHandler implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(WebhookExceptionHandler.class);

    @Override
    public Response toResponse(Exception exception) {
        LOG.errorf(exception, "Unhandled exception in webhook processing");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", "error");
        errorResponse.put("message", "An error occurred processing the webhook");
        errorResponse.put("error", exception.getMessage());

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorResponse)
                .build();
    }
}
