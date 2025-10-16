package ao.co.oportunidade.webhook.dto;

import ao.co.oportunidade.dto.DTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for WebhookEvent exposed in API layer.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WebhookEventDTO extends DTO {

    private UUID id;
    private String appypayTransactionId;
    private String merchantTransactionId;
    private String webhookType;
    private String processingStatus;
    private String payload;
    private Instant receivedAt;
    private Instant processedAt;
    private int retryCount;
    private String errorMessage;
    private Instant createdDate;
    private Instant updatedDate;
}
