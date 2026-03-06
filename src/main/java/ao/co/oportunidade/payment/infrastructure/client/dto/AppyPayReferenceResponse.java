package ao.co.oportunidade.payment.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * AppyPay wire format for reference creation response.
 */
public class AppyPayReferenceResponse {

    @JsonProperty("references")
    private List<ReferenceResult> references;

    public List<ReferenceResult> getReferences() { return references; }
    public void setReferences(List<ReferenceResult> references) { this.references = references; }

    public static class ReferenceResult {
        @JsonProperty("referenceNumber")
        private String referenceNumber;

        @JsonProperty("code")
        private int code;

        @JsonProperty("message")
        private String message;

        public String getReferenceNumber() { return referenceNumber; }
        public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
        public int getCode() { return code; }
        public void setCode(int code) { this.code = code; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
