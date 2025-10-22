package ao.co.oportunidade.odoo.dto;



import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class OdooPaymentRequest {

    // Getters and Setters
    @JsonProperty("event_type")
    private String eventType = "payment.create";

    @JsonProperty("model")
    private String model = "account.payment";

    @JsonProperty("data")
    private PaymentData data;

    public OdooPaymentRequest() {
    }

    public OdooPaymentRequest(PaymentData data) {
        this.data = data;
    }

    public static class PaymentData {
        @JsonProperty("amount")
        private String amount;

        @JsonProperty("currency_id")
        private Integer currencyId;

        @JsonProperty("partner_id")
        private Integer partnerId;

        @JsonProperty("payment_reference")
        private String paymentReference;

        @JsonProperty("payment_date")
        private String paymentDate;

        @JsonProperty("payment_type")
        private String paymentType = "inbound";

        @JsonProperty("journal_id")
        private Integer journalId;

        @JsonProperty("payment_method_id")
        private Integer paymentMethodId;

        @JsonProperty("communication")
        private String communication;  // Payment memo/note

        // Getters and Setters
        public String getAmount() {
            return amount;
        }

        public void setAmount(String amount) {
            this.amount = amount;
        }

        public Integer getCurrencyId() {
            return currencyId;
        }

        public void setCurrencyId(Integer currencyId) {
            this.currencyId = currencyId;
        }

        public Integer getPartnerId() {
            return partnerId;
        }

        public void setPartnerId(Integer partnerId) {
            this.partnerId = partnerId;
        }

        public String getPaymentReference() {
            return paymentReference;
        }

        public void setPaymentReference(String paymentReference) {
            this.paymentReference = paymentReference;
        }

        public String getPaymentDate() {
            return paymentDate;
        }

        public void setPaymentDate(String paymentDate) {
            this.paymentDate = paymentDate;
        }

        public String getPaymentType() {
            return paymentType;
        }

        public void setPaymentType(String paymentType) {
            this.paymentType = paymentType;
        }

        public Integer getJournalId() {
            return journalId;
        }

        public void setJournalId(Integer journalId) {
            this.journalId = journalId;
        }

        public Integer getPaymentMethodId() {
            return paymentMethodId;
        }

        public void setPaymentMethodId(Integer paymentMethodId) {
            this.paymentMethodId = paymentMethodId;
        }

        public String getCommunication() {
            return communication;
        }

        public void setCommunication(String communication) {
            this.communication = communication;
        }
    }
}