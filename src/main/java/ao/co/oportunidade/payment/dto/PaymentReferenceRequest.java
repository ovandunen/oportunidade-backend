package ao.co.oportunidade.payment.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * Request DTO for issuing a MultiCaixa payment reference.
 * Used for both text and QR code reference issuance.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentReferenceRequest {

    /**
     * Merchant/order transaction ID (links to order when webhook arrives).
     */
    private String merchantTransactionId;

    /**
     * Payment amount in AOA.
     */
    private BigDecimal amount;

    /**
     * Employer reference code (from EmployerReference) – identifies the payer.
     * Optional; default entity code used if not provided.
     */
    private String referenceCode;

    /**
     * Entity code for MultiCaixa (e.g. "00123").
     * Defaults to configured value if not provided.
     */
    private String entityCode;
}
