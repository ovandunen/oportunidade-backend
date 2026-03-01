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

import static solutions.envision.resource.Resource.API_VERSION_PATH;

/**
 * REST resource for issuing MultiCaixa payment references.
 * Provides two alternatives: text (for ATM display) and QR code (for POS/mobile).
 * AppyPay webhook handling remains unchanged.
 */
@Path(API_VERSION_PATH + "/payment-references")
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
    public Response issueQrReference(PaymentReferenceRequest request) {
        PaymentReferenceQrResponse response = qrService.issueReferenceQr(request);
        return Response.ok(response).build();
    }
}
