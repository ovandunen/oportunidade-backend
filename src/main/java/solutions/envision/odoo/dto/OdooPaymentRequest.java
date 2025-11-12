package solutions.envision.odoo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

@Setter
@Getter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OdooPaymentRequest {

    @JsonProperty("event_type")
    private String eventType = "payment.create";

    @JsonProperty("model")
    private String model = "account.payment";

    @JsonProperty("data")
    private PaymentData payment;

    public OdooPaymentRequest() {
    }

    public OdooPaymentRequest(PaymentData payment) {
        this.payment = payment;
    }

    @Getter
    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PaymentData {
        @JsonProperty("amount")
        private String amount;

        @JsonProperty("currency_id")
        private Integer currencyId = 1;  // Default to 1 (USD)

        @JsonProperty("partner_id")
        private Integer partnerId;

        @JsonProperty("partner_name")
        private String partnerName;  // Add this - emulator uses it if partner_id not found

        @JsonProperty("ref")
        private String paymentReference;

        @JsonProperty("payment_date")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private LocalDate paymentDate;

        @JsonProperty("payment_type")
        private String paymentType = "inbound";

        @JsonProperty("payment_method_id")
        private Integer paymentMethodId = 1;  // Default to 1

        @JsonProperty("communication")
        private String communication;

        @JsonProperty("state")
        private String state = "posted";  // draft or posted
    }
}