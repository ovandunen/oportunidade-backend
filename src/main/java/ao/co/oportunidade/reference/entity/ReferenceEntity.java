package ao.co.oportunidade.reference.entity;

import solutions.envision.entity.DomainEntity;
import ao.co.oportunidade.order.model.AmountDomainValue;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

        @Entity
        @Table(name = "reference")
        @NamedQueries({
                @NamedQuery(
                        name = ReferenceEntity.FIND_ALL,
                        query = "SELECT r FROM ReferenceEntity r"
                ),
                @NamedQuery(
                        name = ReferenceEntity.EMPLOYEE_FIND_BY_REFERENCE,
                        query = "SELECT r FROM ReferenceEntity r WHERE r.id = :id"
                )
})
public class ReferenceEntity extends DomainEntity {

    public static final String FIND_ALL = "Reference.findAll";
    public static final String EMPLOYEE_FIND_BY_REFERENCE = "Employee.findByReference";
    public static final String PRIMARY_KEY = "id";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "entity")
    private String entity;

    @Column(name = "reference_number")
    private String referenceNumber;

    private String currency;

    @ElementCollection
    @CollectionTable(name = "reference_amounts", joinColumns = @JoinColumn(name = "reference_id"))
    private Collection<AmountDomainValue> amounts;

    @Column(name = "min_amount")
    private Double minAmount;

    @Column(name = "max_amount")
    private Double maxAmount;

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "expiration_date")
    private Instant expirationDate;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "created_date")
    private Instant createdDate;

    @Column(name = "updated_date")
    private Instant updatedDate;

}
