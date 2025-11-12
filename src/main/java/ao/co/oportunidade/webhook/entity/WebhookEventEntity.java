package ao.co.oportunidade.webhook.entity;

import solutions.envision.entity.DomainEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity for WebhookEvent persistence.
 * Separated from domain model following DDD principles.
 */
@Entity
@Table(name = "webhook_events", indexes = {
    @Index(name = "idx_webhook_appypay_tx_id", columnList = "appypayTransactionId", unique = true),
    @Index(name = "idx_webhook_status", columnList = "processingStatus"),
    @Index(name = "idx_webhook_received", columnList = "received_at")
})
@NamedQueries({
    @NamedQuery(
        name = WebhookEventEntity.FIND_ALL,
        query = "SELECT we FROM WebhookEventEntity we"
    ),
    @NamedQuery(
        name = WebhookEventEntity.FIND_BY_ID,
        query = "SELECT we FROM WebhookEventEntity we WHERE we.id = :id"
    ),
    @NamedQuery(
        name = WebhookEventEntity.FIND_BY_APPYPAY_TX_ID,
        query = "SELECT we FROM WebhookEventEntity we WHERE we.appypayTransactionId = :appypayTxId"
    )
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WebhookEventEntity extends DomainEntity {

    public static final String FIND_ALL = "WebhookEvent.findAll";
    public static final String FIND_BY_ID = "WebhookEvent.findById";
    public static final String FIND_BY_APPYPAY_TX_ID = "WebhookEvent.findByAppyPayTxId";
    public static final String PRIMARY_KEY = "id";

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "appypayTransactionId", nullable = false, unique = true, length = 100)
    private String appypayTransactionId;

    @Column(name = "merchant_transaction_id", length = 100)
    private String merchantTransactionId;

    @Column(name = "webhook_type", nullable = false, length = 50)
    private String webhookType;

    @Column(name = "processingStatus", nullable = false, length = 20)
    private String processingStatus;

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
        updatedDate = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDate = Instant.now();
    }
}
