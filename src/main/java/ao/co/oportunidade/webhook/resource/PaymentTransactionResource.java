package ao.co.oportunidade.webhook.resource;

import ao.co.oportunidade.Resource;
import ao.co.oportunidade.webhook.PaymentTransaction;
import ao.co.oportunidade.webhook.PaymentTransactionService;
import ao.co.oportunidade.webhook.dto.PaymentTransactionDTO;
import ao.co.oportunidade.webhook.dto.PaymentTransactionDtoMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Collection;
import java.util.UUID;

/**
 * REST Resource for PaymentTransaction management following DDD principles.
 */
@Path("/payment-transactions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PaymentTransactionResource extends Resource<PaymentTransaction, PaymentTransactionService> {

    @Inject
    private PaymentTransactionDtoMapper mapper;

    /**
     * Get all payment transactions.
     *
     * @return collection of payment transaction DTOs
     */
    @GET
    public Collection<PaymentTransactionDTO> getAllTransactions() {
        return getDomainService().getAllDomains().stream()
                .map(mapper::mapToDto)
                .toList();
    }

    /**
     * Get transaction by AppyPay transaction ID.
     *
     * @param appypayTxId the AppyPay transaction ID
     * @return transaction DTO or 404 if not found
     */
    @GET
    @Path("/appypay/{appypayTxId}")
    public Response getTransactionByAppyPayTxId(@PathParam("appypayTxId") String appypayTxId) {
        return getDomainService().findByAppyPayTransactionId(appypayTxId)
                .map(mapper::mapToDto)
                .map(dto -> Response.ok(dto).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    /**
     * Get transactions by order ID.
     *
     * @param orderId the order ID
     * @return collection of transaction DTOs
     */
    @GET
    @Path("/order/{orderId}")
    public Collection<PaymentTransactionDTO> getTransactionsByOrderId(@PathParam("orderId") UUID orderId) {
        return getDomainService().findByOrderId(orderId).stream()
                .map(mapper::mapToDto)
                .toList();
    }

    /**
     * Create a new payment transaction.
     *
     * @param transactionDTO the transaction DTO
     * @return created response
     */
    @POST
    public Response createTransaction(PaymentTransactionDTO transactionDTO) {
        PaymentTransaction transaction = mapper.mapToDomain(transactionDTO);
        getDomainService().saveDomain(transaction);
        return Response.status(Response.Status.CREATED)
                .entity(mapper.mapToDto(transaction))
                .build();
    }
}
