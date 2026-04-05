package ao.co.oportunidade.payment.resource;

import ao.co.oportunidade.payment.initiate.PaymentService;
import ao.co.oportunidade.payment.initiate.dto.PaymentInitiateRequest;
import ao.co.oportunidade.payment.initiate.dto.PaymentInitiateResponse;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/payments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Payments", description = "Paywall checkout initiation")
public class PaymentResource {

    @Inject
    PaymentService paymentService;

    @Inject
    SecurityIdentity identity;

    @POST
    @Path("/initiate")
    @RolesAllowed("user")
    @Operation(summary = "Start payment for a billable resource")
    public PaymentInitiateResponse initiate(@Valid PaymentInitiateRequest request) {
        String userId = identity.getPrincipal().getName();
        return paymentService.initiate(userId, request);
    }
}
