package ao.co.oportunidade.webhook.resource;

import ao.co.oportunidade.Resource;
import ao.co.oportunidade.webhook.Order;
import ao.co.oportunidade.webhook.OrderService;
import ao.co.oportunidade.webhook.dto.OrderDTO;
import ao.co.oportunidade.webhook.dto.OrderDtoMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Collection;

/**
 * REST Resource for Order management following DDD principles.
 */
@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderResource extends Resource<Order, OrderService> {

    @Inject
    private OrderDtoMapper mapper;

    /**
     * Get all orders.
     *
     * @return collection of order DTOs
     */
    @GET
    public Collection<OrderDTO> getAllOrders() {
        return getDomainService().getAllDomains().stream()
                .map(mapper::mapToDto)
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
                .map(mapper::mapToDto)
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
        Order order = mapper.mapToDomain(orderDTO);
        getDomainService().createDomain(order);
        return Response.status(Response.Status.CREATED)
                .entity(mapper.mapToDto(order))
                .build();
    }
}
