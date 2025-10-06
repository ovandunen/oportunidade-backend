package ao.co.oportunidade.webhook.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Instant;

/**
 * DTO representing a transaction event in the AppyPay webhook payload.
 * Tracks the history of status changes for a transaction.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionEvent {

    /**
     * Event type (e.g., "StatusChanged", "PaymentReceived")
     */
    @JsonProperty("type")
    private String type;

    /**
     * Event status
     */
    @JsonProperty("status")
    private String status;

    /**
     * Event message or description
     */
    @JsonProperty("message")
    private String message;

    /**
     * Timestamp when the event occurred
     */
    @JsonProperty("timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant timestamp;

    /**
     * Additional event data
     */
    @JsonProperty("data")
    private Object data;
}
