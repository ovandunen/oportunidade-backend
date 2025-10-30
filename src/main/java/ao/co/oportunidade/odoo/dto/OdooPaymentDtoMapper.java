package ao.co.oportunidade.odoo.dto;

import ao.co.oportunidade.dto.DtoMapper;
import ao.co.oportunidade.webhook.PaymentTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * MapStruct mapper for OdooPaymentRequest.PaymentData and PaymentTransaction domain.
 */
@Mapper(componentModel = "cdi")
public interface OdooPaymentDtoMapper extends DtoMapper<OdooPaymentRequest.PaymentData, PaymentTransaction> {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderId", ignore = true)
    @Mapping(target = "appypayTransactionId", ignore = true)
    @Mapping(source = "amount", target = "amount", qualifiedByName = "stringToBigDecimal")
    @Mapping(target = "currency", constant = "AOA")
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "paymentMethod", ignore = true)
    @Mapping(source = "paymentReference", target = "referenceNumber")
    @Mapping(target = "referenceEntity", constant = "ODOO")
    @Mapping(source = "paymentDate", target = "transactionDate", qualifiedByName = "localDateToInstant")
    @Mapping(target = "createdDate", expression = "java(java.time.Instant.now())")
    @Mapping(target = "updatedDate", ignore = true)
    @Mapping(target = "errorMessage", ignore = true)
    @Mapping(target = "paymentId", ignore = true)
    @Override
    PaymentTransaction mapToDomain(OdooPaymentRequest.PaymentData paymentData);

    @Mapping(source = "transaction.amount", target = "amount", qualifiedByName = "bigDecimalToString")
    @Mapping(target = "currencyId", constant = "1")
    @Mapping(source = "transaction.referenceNumber", target = "paymentReference")
    @Mapping(source = "transaction.transactionDate", target = "paymentDate", qualifiedByName = "instantToLocalDate")
    @Mapping(target = "paymentType", constant = "inbound")
    @Mapping(target = "paymentMethodId", constant = "1")
    @Mapping(target = "state", constant = "posted")
    @Mapping(target = "partnerId", ignore = true)
    @Mapping(source = "customerName", target = "partnerName")
    @Mapping(source = "transaction", target = "communication", qualifiedByName = "buildCommunication")
    OdooPaymentRequest.PaymentData mapToDto(PaymentTransaction transaction, String customerName);

    @Named("bigDecimalToString")
    default String bigDecimalToString(BigDecimal value) {
        return value != null ? value.toPlainString() : null;
    }

    @Named("stringToBigDecimal")
    default BigDecimal stringToBigDecimal(String value) {
        return value != null ? new BigDecimal(value) : null;
    }

    @Named("instantToLocalDate")
    default LocalDate instantToLocalDate(Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.atZone(ZoneId.systemDefault()).toLocalDate();
    }

    @Named("localDateToInstant")
    default Instant localDateToInstant(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    @Named("buildCommunication")
    default String buildCommunication(PaymentTransaction transaction) {
        return String.format("Payment %s via %s",
                transaction.getReferenceNumber(),
                transaction.getPaymentMethod());
    }
}