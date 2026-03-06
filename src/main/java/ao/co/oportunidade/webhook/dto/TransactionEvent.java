package ao.co.oportunidade.webhook.dto;

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
     * Timestamp when the event occurred (ISO-8601 e.g. 2026-03-05T19:58:15Z)
     */
    @JsonProperty("timestamp")
    private Instant timestamp;

    /**
     * Additional event data
     */
    @JsonProperty("data")
    private Object data;
}
