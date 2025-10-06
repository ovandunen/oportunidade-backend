package ao.co.oportunidade.webhook.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * DTO representing customer information in AppyPay webhook payload.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerInfo {

    /**
     * Customer name
     */
    @JsonProperty("name")
    private String name;

    /**
     * Customer email
     */
    @JsonProperty("email")
    private String email;

    /**
     * Customer phone number
     */
    @JsonProperty("phone")
    private String phone;

    /**
     * Customer document number (e.g., ID, passport)
     */
    @JsonProperty("documentNumber")
    private String documentNumber;

    /**
     * Customer document type
     */
    @JsonProperty("documentType")
    private String documentType;
}
