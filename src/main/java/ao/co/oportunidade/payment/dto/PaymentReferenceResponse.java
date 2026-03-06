package ao.co.oportunidade.payment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;

/**
 * Response DTO for payment reference issuance (text format).
 */
@Schema(description = "Payment reference for MultiCaixa ATM/agent display")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentReferenceResponse {

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

    /**
     * Reference valid until (for MultiCaixa payment window).
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant dueDate;

    /**
     * Merchant transaction ID (order link).
     */
    private String merchantTransactionId;
}
