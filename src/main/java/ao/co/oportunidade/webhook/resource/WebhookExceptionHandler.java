package ao.co.oportunidade.webhook.resource;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for webhook-related exceptions.
 *
 * Client errors (4xx) from JAX-RS deserialization (e.g. malformed JSON) are
 * passed through with their original status code. Only genuine server errors
 * are mapped to 500.
 */
@Provider
public class WebhookExceptionHandler implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(WebhookExceptionHandler.class);

    @Override
    public Response toResponse(Exception exception) {
        // WebApplicationException carries its own HTTP status (e.g. 400 for bad JSON).
        // If it is a client error, pass it through unchanged — wrapping it as 500 would
        // hide the real cause and break tests that assert on the status code.
        if (exception instanceof WebApplicationException wae) {
            int status = wae.getResponse().getStatus();
            if (status < 500) {
                LOG.warnf("Client error in webhook processing: %s", exception.getMessage());
                return Response.status(status)
                        .entity(buildErrorBody("Invalid request", exception.getMessage()))
                        .build();
            }
        }

        LOG.errorf(exception, "Unhandled exception in webhook processing");

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(buildErrorBody("An error occurred processing the webhook", exception.getMessage()))
                .build();
    }

    private Map<String, Object> buildErrorBody(String message, String error) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "error");
        body.put("message", message);
        body.put("error", error);
        return body;
    }
}