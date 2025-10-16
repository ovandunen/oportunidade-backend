package ao.co.oportunidade.webhook.dto;

import ao.co.oportunidade.webhook.PaymentTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for PaymentTransactionDTO and PaymentTransaction domain.
 */
@Mapper(componentModel = "cdi")
public interface PaymentTransactionDtoMapper extends ao.co.oportunidade.dto.DtoMapper<PaymentTransactionDTO, PaymentTransaction> {

    @Mapping(target = "status", source = "status")
    PaymentTransactionDTO mapToDto(PaymentTransaction domain);

    @Mapping(target = "status", source = "status")
    PaymentTransaction mapToDomain(PaymentTransactionDTO dto);

    default String mapStatusToString(PaymentTransaction.TransactionStatus status) {
        return status != null ? status.name() : null;
    }

    default PaymentTransaction.TransactionStatus mapStringToStatus(String status) {
        return status != null ? PaymentTransaction.TransactionStatus.valueOf(status) : null;
    }
}
