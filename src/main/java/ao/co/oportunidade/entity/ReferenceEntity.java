package ao.co.oportunidade.entity;

import ao.co.oportunidade.DomainEntity;
import ao.co.oportunidade.valueobject.AmountDomainValue;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

        @Entity
        @Table(name = "references")
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
    private UUID id;

    private String entity;
    private String referenceNumber;
    private String currency;
    private List<AmountDomainValue> amounts;
    private Double minAmount;
    private Double maxAmount;
    private Instant startDate;
    private Instant expirationDate;
    private Boolean isActive;
    private String createdBy;
    private String updatedBy;
    private Instant createdDate;
    private Instant updatedDate;

}
