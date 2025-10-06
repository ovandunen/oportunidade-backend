package ao.co.oportunidade.webhook.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * DTO representing the response status in AppyPay webhook payload.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResponseStatus {

    /**
     * Response code
     */
    @JsonProperty("code")
    private String code;

    /**
     * Response message
     */
    @JsonProperty("message")
    private String message;

    /**
     * Success flag
     */
    @JsonProperty("success")
    private Boolean success;

    /**
     * Error code if applicable
     */
    @JsonProperty("errorCode")
    private String errorCode;

    /**
     * Error details if applicable
     */
    @JsonProperty("errorDetails")
    private String errorDetails;
}
