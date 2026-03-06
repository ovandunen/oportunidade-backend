package ao.co.oportunidade.payment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import solutions.envision.entity.DomainEntity;

import java.time.Instant;
import java.util.UUID;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentReferenceEntity extends DomainEntity {

    public static final String FIND_ALL = "PaymentReference.findAll";
    public static final String FIND_BY_ID = "PaymentReference.findById";

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;
    /**
     * Generated 9-digit reference number for MultiCaixa.
     */
    private String referenceNumber;

    /**
     * Entity code (e.g. "00123").
     */
    private String entityCode;

    /**
     * Full reference as plain text for display/ATM.
     */
    private String referenceText;

    /**
     * Amount in AOA.
     */
    private java.math.BigDecimal amount;

    /**
     * Currency (AOA).
     */
    private String currency;

    private Instant dueDate;

    /**
     * Merchant transaction ID (order link).
     */
    private String merchantTransactionId;
}
