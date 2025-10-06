package ao.co.oportunidade.webhook.entity;

import ao.co.oportunidade.webhook.PaymentTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for PaymentTransaction domain and PaymentTransactionEntity.
 */
@Mapper(componentModel = "cdi")
public interface PaymentTransactionEntityMapper extends ao.co.oportunidade.entity.EntityMapper<PaymentTransaction, PaymentTransactionEntity> {

    @Mapping(target = "status", source = "status")
    PaymentTransactionEntity mapToEntity(PaymentTransaction domain);

    @Mapping(target = "status", source = "status")
    PaymentTransaction mapToDomain(PaymentTransactionEntity entity);

    default String mapStatusToString(PaymentTransaction.TransactionStatus status) {
        return status != null ? status.name() : null;
    }

    default PaymentTransaction.TransactionStatus mapStringToStatus(String status) {
        return status != null ? PaymentTransaction.TransactionStatus.valueOf(status) : null;
    }
}
