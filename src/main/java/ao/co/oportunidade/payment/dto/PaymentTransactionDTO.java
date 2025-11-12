package ao.co.oportunidade.payment.dto;

import solutions.envision.dto.DTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for PaymentTransaction exposed in API layer.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransactionDTO extends DTO {

    private UUID id;
    private UUID orderId;
    private String appypayTransactionId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String paymentMethod;
    private String referenceNumber;
    private String referenceEntity;
    private Instant transactionDate;
    private Instant createdDate;
    private Instant updatedDate;
    private String errorMessage;
}
