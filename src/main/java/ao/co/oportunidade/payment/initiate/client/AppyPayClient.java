package ao.co.oportunidade.payment.initiate.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * AppyPay emulator / gateway — creates a hosted payment session.
 */
@RegisterRestClient(configKey = "appypay-emulator")
@Path("/v1/payment-sessions")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface AppyPayClient {

    @POST
    AppyPaySessionResponse createPaymentSession(AppyPaySessionRequest request);
}
