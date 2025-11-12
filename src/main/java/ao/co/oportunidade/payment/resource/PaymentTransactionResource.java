package ao.co.oportunidade.payment.resource;

import ao.co.oportunidade.payment.model.PaymentTransaction;
import ao.co.oportunidade.payment.service.PaymentTransactionService;
import ao.co.oportunidade.payment.dto.PaymentTransactionDTO;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import solutions.envision.resource.ServiceResource;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static solutions.envision.resource.Resource.API_VERSION_PATH;

/**
 * REST Resource for PaymentTransaction management following DDD principles.
 */
@Path(API_VERSION_PATH +"/payment-transactions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PaymentTransactionResource extends ServiceResource<PaymentTransactionDTO, PaymentTransaction,PaymentTransactionService> {


    /**
     * Get all payment transactions.
     *
     * @return collection of payment transaction DTOs
     */
    @GET
    public Response getAllTransactions() {
        final List<PaymentTransactionDTO> payments = getService().getAllDomains().stream()
                .map(getMapper()::mapToDto)
                .toList();

        if(payments.isEmpty()) {
           return Response.status(Response.Status.NO_CONTENT).build();
        }
        return Response.ok(payments).build();
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
                .map(getMapper()::mapToDto)
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
                .map(getMapper()::mapToDto)
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
        PaymentTransaction transaction = getMapper().mapToDomain(transactionDTO);
        getService().saveDomain(transaction);
        return Response.status(Response.Status.CREATED)
                .entity(getMapper().mapToDto(transaction))
                .build();
    }
}
