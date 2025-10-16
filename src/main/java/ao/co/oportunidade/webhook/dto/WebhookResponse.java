package ao.co.oportunidade.webhook.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * DTO representing the webhook endpoint response.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookResponse {

    /**
     * Status of webhook receipt
     */
    @JsonProperty("status")
    private String status;

    /**
     * Response message
     */
    @JsonProperty("message")
    private String message;

    /**
     * Webhook event ID for tracking
     */
    @JsonProperty("eventId")
    private String eventId;

    /**
     * Create a success response
     */
    public static WebhookResponse success(String eventId) {
        return WebhookResponse.builder()
                .status("received")
                .message("Webhook received and queued for processing")
                .eventId(eventId)
                .build();
    }

    /**
     * Create an error response
     */
    public static WebhookResponse error(String message) {
        return WebhookResponse.builder()
                .status("error")
                .message(message)
                .build();
    }
}
