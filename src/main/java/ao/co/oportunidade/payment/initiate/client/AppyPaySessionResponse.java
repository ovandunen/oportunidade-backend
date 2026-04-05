package ao.co.oportunidade.payment.initiate.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AppyPaySessionResponse(
        @JsonProperty("paymentUrl") String paymentUrl) {
}
