package ao.co.oportunidade.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentReferenceDto {

    /**
     * Generated 9-digit reference number for MultiCaixa.
     */
    private String referenceNumber;

    /**
     * Entity code (e.g. "00123").
     */
    private String entityCode;

    /**
     * Full reference as plain text for display/ATM.
     */
    private String referenceText;

    /**
     * Amount in AOA.
     */
    private java.math.BigDecimal amount;

    /**
     * Currency (AOA).
     */
    private String currency;

    private Instant dueDate;

    /**
     * Merchant transaction ID (order link).
     */
    private String merchantTransactionId;
}
