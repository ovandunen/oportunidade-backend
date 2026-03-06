package ao.co.oportunidade.payment.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * AppyPay wire format for reference creation request.
 */
public class AppyPayReferenceRequest {

    @JsonProperty("paymentMethod")
    private String paymentMethod;

    @JsonProperty("references")
    private List<ReferenceItem> references;

    @JsonProperty("createdBy")
    private String createdBy;

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public List<ReferenceItem> getReferences() { return references; }
    public void setReferences(List<ReferenceItem> references) { this.references = references; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public static class ReferenceItem {
        @JsonProperty("referenceNumber")
        private String referenceNumber;

        @JsonProperty("merchantTransactionId")
        private String merchantTransactionId;

        @JsonProperty("currency")
        private String currency;

        @JsonProperty("amounts")
        private List<AmountItem> amounts;

        @JsonProperty("expirationDate")
        private String expirationDate;

        public String getReferenceNumber() { return referenceNumber; }
        public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
        public String getMerchantTransactionId() { return merchantTransactionId; }
        public void setMerchantTransactionId(String merchantTransactionId) { this.merchantTransactionId = merchantTransactionId; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public List<AmountItem> getAmounts() { return amounts; }
        public void setAmounts(List<AmountItem> amounts) { this.amounts = amounts; }
        public String getExpirationDate() { return expirationDate; }
        public void setExpirationDate(String expirationDate) { this.expirationDate = expirationDate; }
    }

    public static class AmountItem {
        @JsonProperty("amount")
        private Number amount;

        @JsonProperty("descriptionLine1")
        private String descriptionLine1;

        @JsonProperty("descriptionLine2")
        private String descriptionLine2;

        public Number getAmount() { return amount; }
        public void setAmount(Number amount) { this.amount = amount; }
        public String getDescriptionLine1() { return descriptionLine1; }
        public void setDescriptionLine1(String descriptionLine1) { this.descriptionLine1 = descriptionLine1; }
        public String getDescriptionLine2() { return descriptionLine2; }
        public void setDescriptionLine2(String descriptionLine2) { this.descriptionLine2 = descriptionLine2; }
    }
}
