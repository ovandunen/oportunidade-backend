package ao.co.oportunidade.order.resource;

import ao.co.oportunidade.order.service.OrderService;
import ao.co.oportunidade.order.model.Order;
import ao.co.oportunidade.order.dto.OrderDTO;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import solutions.envision.resource.ServiceResource;

import java.util.Collection;

import static solutions.envision.resource.Resource.API_VERSION_PATH;

/**
 * REST Resource for Order management following DDD principles.
 */
@Path(API_VERSION_PATH +"/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderResource extends ServiceResource<OrderDTO, Order, OrderService> {

    /**
     * Get all orders.
     *
     * @return collection of order DTOs
     */
    @GET
    public Collection<OrderDTO> getAllOrders() {
        return getService().getAllDomains().stream()
                .map(getMapper()::mapToDto)
                .toList();
    }

    /**
     * Get order by merchant transaction ID.
     *
     * @param merchantTxId the merchant transaction ID
     * @return order DTO or 404 if not found
     */
    @GET
    @Path("/merchant/{merchantTxId}")
    public Response getOrderByMerchantTxId(@PathParam("merchantTxId") String merchantTxId) {
        return getDomainService().findByMerchantTransactionId(merchantTxId)
                .map(getMapper()::mapToDto)
                .map(dto -> Response.ok(dto).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    /**
     * Create a new order.
     *
     * @param orderDTO the order DTO
     * @return created response
     */
    @POST
    public Response createOrder(OrderDTO orderDTO) {
        Order order = getMapper().mapToDomain(orderDTO);
        getService().saveDomain(order);
        return Response.status(Response.Status.CREATED)
                .entity(getMapper().mapToDto(order))
                .build();
    }
}
