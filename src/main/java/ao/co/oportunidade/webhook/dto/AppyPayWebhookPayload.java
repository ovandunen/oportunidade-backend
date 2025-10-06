package ao.co.oportunidade.webhook.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * DTO representing the AppyPay webhook payload.
 * This is the main payload received from AppyPay when a payment status changes.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppyPayWebhookPayload {

    /**
     * Unique identifier for the transaction in AppyPay system
     */
    @JsonProperty("id")
    private String id;

    /**
     * Merchant's transaction identifier (order ID)
     */
    @JsonProperty("merchantTransactionId")
    private String merchantTransactionId;

    /**
     * Type of transaction (e.g., "Charge", "Refund")
     */
    @JsonProperty("type")
    private String type;

    /**
     * Transaction amount
     */
    @JsonProperty("amount")
    private BigDecimal amount;

    /**
     * Currency code (e.g., "AOA")
     */
    @JsonProperty("currency")
    private String currency;

    /**
     * Payment status (e.g., "Success", "Pending", "Failed")
     */
    @JsonProperty("status")
    private String status;

    /**
     * Payment method used (e.g., "REF" for reference)
     */
    @JsonProperty("paymentMethod")
    private String paymentMethod;

    /**
     * Reference information for payment
     */
    @JsonProperty("reference")
    private ReferenceInfo reference;

    /**
     * Transaction events history
     */
    @JsonProperty("events")
    private List<TransactionEvent> events;

    /**
     * Response status information
     */
    @JsonProperty("responseStatus")
    private ResponseStatus responseStatus;

    /**
     * Customer information
     */
    @JsonProperty("customer")
    private CustomerInfo customer;

    /**
     * Date when transaction was created
     */
    @JsonProperty("createdDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant createdDate;

    /**
     * Date when transaction was last updated
     */
    @JsonProperty("updatedDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant updatedDate;

    /**
     * Additional metadata
     */
    @JsonProperty("metadata")
    private Object metadata;
}
