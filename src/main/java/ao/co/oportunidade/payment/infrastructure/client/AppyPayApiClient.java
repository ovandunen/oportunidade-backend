package ao.co.oportunidade.payment.infrastructure.client;

import ao.co.oportunidade.payment.infrastructure.client.dto.AppyPayReferenceRequest;
import ao.co.oportunidade.payment.infrastructure.client.dto.AppyPayReferenceResponse;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "appypay-api")
@Path("/v1")
public interface AppyPayApiClient {

    @POST
    @Path("/references")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    AppyPayReferenceResponse createReferences(AppyPayReferenceRequest request);
}
