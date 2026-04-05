package ao.co.oportunidade.payment.initiate.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AppyPaySessionRequest(
        @JsonProperty("merchantTransactionId") String merchantTransactionId,
        @JsonProperty("resourceId") String resourceId,
        @JsonProperty("resourceType") String resourceType) {
}
