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
import java.time.format.DateTimeFormatter;

/**
 * MapStruct mapper for OdooPaymentRequest.PaymentData and PaymentTransaction domain.
 */
@Mapper(componentModel = "cdi")
public interface OdooPaymentDtoMapper extends DtoMapper<OdooPaymentRequest.PaymentData,PaymentTransaction> {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderId", ignore = true)
    @Mapping(target = "appypayTransactionId", ignore = true)
    @Mapping(source = "amount", target = "amount", qualifiedByName = "stringToBigDecimal")
    @Mapping(target = "currency", constant = "AOA")  // Set default or map from currencyId
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "paymentMethod", ignore = true)  // Or map from paymentMethodId
    @Mapping(source = "paymentReference", target = "referenceNumber")
    @Mapping(target = "referenceEntity", constant = "ODOO")
    @Mapping(source = "paymentDate", target = "transactionDate", qualifiedByName = "stringToInstant")
    @Mapping(target = "createdDate", expression = "java(java.time.Instant.now())")
    @Mapping(target = "updatedDate", ignore = true)
    @Mapping(target = "errorMessage", ignore = true)
    @Override
    PaymentTransaction mapToDomain(OdooPaymentRequest.PaymentData paymentData);

    @Mapping(source = "amount", target = "amount", qualifiedByName = "bigDecimalToString")
    @Mapping(target = "currencyId", constant = "1")  // Set default currency ID
    @Mapping(source = "referenceNumber", target = "paymentReference")
    @Mapping(source = "transactionDate", target = "paymentDate", qualifiedByName = "instantToString")
    @Mapping(target = "paymentType", constant = "inbound")
    @Mapping(target = "journalId", constant = "1")  // Set default journal ID
    @Mapping(target = "paymentMethodId", constant = "1")  // Set default payment method ID
    @Mapping(target = "partnerId", ignore = true)  // Map separately if needed
    @Mapping(target = "communication", expression = "java(buildCommunication(transaction))")
    @Override
    OdooPaymentRequest.PaymentData mapToDto(PaymentTransaction transaction);

    // Helper method for communication
    default String buildCommunication(PaymentTransaction transaction) {
        return String.format(
                "Payment from AppyPay - Ref: %s, TX: %s",
                transaction.getReferenceNumber(),
                transaction.getAppypayTransactionId()
        );
    }

    // Custom mapping methods

    @Named("stringToBigDecimal")
    default BigDecimal stringToBigDecimal(String amount) {
        if (amount == null || amount.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(amount);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    @Named("bigDecimalToString")
    default String bigDecimalToString(BigDecimal amount) {
        return amount != null ? amount.toPlainString() : "0.00";
    }

    @Named("stringToInstant")
    default Instant stringToInstant(String dateString) {
        if (dateString == null || dateString.isBlank()) {
            return Instant.now();
        }
        try {
            LocalDate date = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE);
            return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
        } catch (Exception e) {
            return Instant.now();
        }
    }

    @Named("instantToString")
    default String instantToString(Instant instant) {
        if (instant == null) {
            return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        return instant.atZone(ZoneId.systemDefault())
                .toLocalDate()
                .format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
