package ao.co.oportunidade.order.entity;

import ao.co.oportunidade.order.model.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import solutions.envision.entity.EntityMapper;

/**
 * MapStruct mapper for Order domain and OrderEntity.
 */
@Mapper(componentModel = "cdi")
public interface OrderEntityMapper extends EntityMapper<Order, OrderEntity> {

    @Mapping(target = "status", source = "status")
    OrderEntity mapToEntity(Order domain);

    @Mapping(target = "status", source = "status")
    Order mapToDomain(OrderEntity entity);

    default String mapStatusToString(Order.OrderStatus status) {
        return status != null ? status.name() : null;
    }

    default Order.OrderStatus mapStringToStatus(String status) {
        return status != null ? Order.OrderStatus.valueOf(status) : null;
    }
}
