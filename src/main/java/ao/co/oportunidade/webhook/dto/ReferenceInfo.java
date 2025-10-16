package ao.co.oportunidade.webhook.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
     * Due date for the payment reference
     */
    @JsonProperty("dueDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private Instant dueDate;

    /**
     * Start date for the payment reference
     */
    @JsonProperty("startDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private Instant startDate;

    /**
     * Reference status
     */
    @JsonProperty("status")
    private String status;
}
