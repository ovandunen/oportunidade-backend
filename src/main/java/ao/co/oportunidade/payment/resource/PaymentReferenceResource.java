package ao.co.oportunidade.payment.resource;

import ao.co.oportunidade.payment.dto.PaymentReferenceRequest;
import ao.co.oportunidade.payment.dto.PaymentReferenceQrResponse;
import ao.co.oportunidade.payment.dto.PaymentReferenceResponse;
import ao.co.oportunidade.payment.service.PaymentReferenceQrService;
import ao.co.oportunidade.payment.service.PaymentReferenceTextService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import static solutions.envision.resource.Resource.API_VERSION_PATH;

/**
 * REST resource for issuing MultiCaixa payment references.
 * Provides two alternatives: text (for ATM display) and QR code (for POS/mobile).
 * AppyPay webhook handling remains unchanged.
 */
@Path(API_VERSION_PATH + "/payment-references")
@Tag(name = "Payment References", description = "Issue MultiCaixa payment references (text or QR)")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PaymentReferenceResource {

    @Inject
    PaymentReferenceTextService textService;

    @Inject
    PaymentReferenceQrService qrService;

    /**
     * Issue a payment reference as plain text.
     * Use for display at MultiCaixa ATM/agent.
     */
    @POST
    @Path("/text")
    @Operation(summary = "Issue text reference", description = "Returns a 9-digit reference for display at MultiCaixa ATM or agent")
    @APIResponse(responseCode = "200", description = "Reference issued successfully",
            content = @Content(schema = @Schema(implementation = PaymentReferenceResponse.class)))
    public Response issueTextReference(PaymentReferenceRequest request) {
        PaymentReferenceResponse response = textService.issueReference(request);
        return Response.ok(response).build();
    }

    /**
     * Issue a payment reference as QR code (base64 PNG).
     * Use for MultiCaixa POS or QR-enabled payment terminals.
     */
    @POST
    @Path("/qr")
    @Operation(summary = "Issue QR reference", description = "Returns a base64-encoded PNG QR code for MultiCaixa POS or mobile")
    @APIResponse(responseCode = "200", description = "QR reference issued successfully",
            content = @Content(schema = @Schema(implementation = PaymentReferenceQrResponse.class)))
    public Response issueQrReference(PaymentReferenceRequest request) {
        PaymentReferenceQrResponse response = qrService.issueReferenceQr(request);
        return Response.ok(response).build();
    }
}
