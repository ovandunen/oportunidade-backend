package ao.co.oportunidade.payment.dto;

import lombok.*;

/**
 * Response DTO for payment reference issued as QR code.
 * Contains base64-encoded PNG image and reference metadata.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentReferenceQrResponse {

    /**
     * QR code image as base64-encoded PNG (data:image/png;base64,...).
     */
    private String qrCodeBase64;

    /**
     * Reference number for manual entry fallback.
     */
    private String referenceNumber;

    /**
     * Entity code.
     */
    private String entityCode;

    /**
     * Raw reference text encoded in QR (for verification).
     */
    private String referenceText;
}
