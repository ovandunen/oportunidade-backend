package ao.co.oportunidade.payment.dto;

import lombok.*;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Request DTO for issuing a MultiCaixa payment reference.
 * Used for both text and QR code reference issuance.
 */
@Schema(description = "Request to issue a MultiCaixa payment reference")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentReferenceRequest {

    @Schema(description = "Merchant/order transaction ID (links to order when webhook arrives)", example = "order-1234567890", required = true)
    private String merchantTransactionId;

    @Schema(description = "Payment amount in AOA", example = "5000", required = true)
    private BigDecimal amount;

    @Schema(description = "Employer reference code (optional; default entity code used if not provided)")
    private String referenceCode;

    @Schema(description = "Entity code for MultiCaixa (e.g. 00123); defaults to configured value if not provided")
    private String entityCode;
}
