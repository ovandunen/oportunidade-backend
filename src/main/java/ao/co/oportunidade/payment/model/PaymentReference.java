package ao.co.oportunidade.payment.model;

import lombok.Getter;
import lombok.Setter;
import solutions.envision.model.Domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;


public class PaymentReference extends Domain {


    /**
     * Generated 9-digit reference number for MultiCaixa.
     */
    @Getter
    @Setter
    private String referenceNumber;

    /**
     * Entity code (e.g. "00123").
     */
    @Getter
    @Setter
    private String entityCode;


    private final UUID id;


    /**
     * Full reference as plain text for display/ATM.
     */
    @Getter
    @Setter
    private String referenceText;


    /**
     * Amount in AOA.
     */
    @Getter
    @Setter
    private java.math.BigDecimal amount;

    /**
     * Currency (AOA).
     */
    @Getter
    @Setter
    private String currency;

    @Getter
    @Setter
    private Instant dueDate;

    public PaymentReference() {
        id = UUID.randomUUID();
    }

    /**
     * Merchant transaction ID (order link).
     */
    @Getter
    @Setter
    private String merchantTransactionId;

    @Override
    public UUID getId() {
        return id;
    }

    /*
    command.getMerchantTransactionId(),
                issued.getReferenceNumber(),
                issued.getEntityCode(),
                command.getAmount(),
                command.getExpiresAt());
     */

    public PaymentReference issue( String referenceNumber,String transactionId, String entityCode,
                      BigDecimal amount, String currency, Instant dueDate) {

        validate(referenceNumber,transactionId,entityCode,amount,currency,dueDate);

        this.referenceNumber = referenceNumber;
        this.merchantTransactionId = transactionId;
        this.entityCode = entityCode;
        this.amount = amount;
        this.currency = currency;
        this.dueDate = dueDate;

        return this;
    }

    void validate(String referenceNumber,String transactionId, String entityCode,
                  BigDecimal amount, String currency, Instant dueDate) throws IllegalArgumentException {

    }
}
