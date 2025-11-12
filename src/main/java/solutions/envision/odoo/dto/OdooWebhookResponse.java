package solutions.envision.odoo.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class OdooWebhookResponse {

    // Getters and Setters
    @JsonProperty("success")
    private Boolean success;

    @JsonProperty("payment_id")
    private Integer paymentId;

    @JsonProperty("message")
    private String message;

    @JsonProperty("error")
    private String error;

}