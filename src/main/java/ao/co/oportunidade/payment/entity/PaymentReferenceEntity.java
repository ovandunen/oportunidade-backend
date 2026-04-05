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
@Table(name = "payment_reference")
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
    @Column(name = "reference_number")
    private String referenceNumber;

    /**
     * Entity code (e.g. "00123").
     */
    @Column(name = "entity_code")
    private String entityCode;

    /**
     * Full reference as plain text for display/ATM.
     */
    @Column(name = "reference_text")
    private String referenceText;

    /**
     * Amount in AOA.
     */
    @Column(precision = 19, scale = 2)
    private java.math.BigDecimal amount;

    /**
     * Currency (AOA).
     */
    private String currency;

    @Column(name = "due_date")
    private Instant dueDate;

    /**
     * Merchant transaction ID (order link).
     */
    @Column(name = "merchant_transaction_id")
    private String merchantTransactionId;
}
