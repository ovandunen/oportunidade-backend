package ao.co.oportunidade.webhook;

import ao.co.oportunidade.Domain;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain model for tracking webhook events.
 * Ensures idempotency and provides debugging capabilities.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookEvent extends Domain {

    private UUID id;
    private String appypayTransactionId;
    private String merchantTransactionId;
    private String webhookType;
    private ProcessingStatus processingStatus;
    private String payload;
    private Instant receivedAt;
    private Instant processedAt;
    private int retryCount;
    private String errorMessage;
    private Instant createdDate;
    private Instant updatedDate;

    @Override
    public UUID getId() {
        return id;
    }

    /**
     * Processing status for webhook events
     */
    public enum ProcessingStatus {
        RECEIVED,
        PROCESSING,
        PROCESSED,
        FAILED,
        DEAD_LETTER
    }
}
