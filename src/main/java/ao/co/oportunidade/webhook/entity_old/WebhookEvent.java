package ao.co.oportunidade.webhook.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity for tracking webhook events to ensure idempotency and provide debugging capabilities.
 * Prevents duplicate processing of the same webhook.
 */
@Entity
@Table(name = "webhook_events", indexes = {
    @Index(name = "idx_webhook_appypay_tx_id", columnList = "appypayTransactionId", unique = true),
    @Index(name = "idx_webhook_status", columnList = "processingStatus"),
    @Index(name = "idx_webhook_received", columnList = "receivedAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookEvent extends PanacheEntityBase {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "appypayTransactionId", nullable = false, unique = true, length = 100)
    private String appypayTransactionId;

    @Column(name = "merchant_transaction_id", length = 100)
    private String merchantTransactionId;

    @Column(name = "webhook_type", nullable = false, length = 50)
    private String webhookType;

    @Enumerated(EnumType.STRING)
    @Column(name = "processingStatus", nullable = false, length = 20)
    private ProcessingStatus processingStatus;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_date", nullable = false)
    private Instant createdDate;

    @Column(name = "updated_date", nullable = false)
    private Instant updatedDate;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        if (createdDate == null) {
            createdDate = now;
        }
        if (receivedAt == null) {
            receivedAt = now;
        }
        if (retryCount == 0) {
            retryCount = 0;
        }
        updatedDate = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDate = Instant.now();
    }

    /**
     * Processing status for webhook events
     */
    public enum ProcessingStatus {
        RECEIVED,       // Webhook received, not yet processed
        PROCESSING,     // Currently being processed
        PROCESSED,      // Successfully processed
        FAILED,         // Processing failed
        DEAD_LETTER     // Moved to dead letter queue after max retries
    }
}
