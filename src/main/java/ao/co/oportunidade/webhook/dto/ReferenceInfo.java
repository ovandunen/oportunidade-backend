package ao.co.oportunidade.webhook.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Instant;

/**
 * DTO representing payment reference information in AppyPay webhook.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferenceInfo {

    /**
     * The payment reference number
     */
    @JsonProperty("referenceNumber")
    private String referenceNumber;

    /**
     * The entity code for the reference
     */
    @JsonProperty("entity")
    private String entity;

    /**
     * Due date for the payment reference (ISO-8601 e.g. 2026-03-06T19:47:20Z)
     */
    @JsonProperty("dueDate")
    private Instant dueDate;

    /**
     * Start date for the payment reference (ISO-8601 e.g. 2026-03-06T19:47:20Z)
     */
    @JsonProperty("startDate")
    private Instant startDate;

    /**
     * Reference status
     */
    @JsonProperty("status")
    private String status;
}
