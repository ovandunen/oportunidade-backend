package ao.co.oportunidade.webhook.dto;

import ao.co.oportunidade.webhook.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for OrderDTO and Order domain.
 */
@Mapper(componentModel = "cdi")
public interface OrderDtoMapper extends ao.co.oportunidade.dto.DtoMapper<OrderDTO, Order> {

    @Mapping(target = "status", source = "status")
    OrderDTO mapToDto(Order domain);

    @Mapping(target = "status", source = "status")
    Order mapToDomain(OrderDTO dto);

    default String mapStatusToString(Order.OrderStatus status) {
        return status != null ? status.name() : null;
    }

    default Order.OrderStatus mapStringToStatus(String status) {
        return status != null ? Order.OrderStatus.valueOf(status) : null;
    }
}
