package ao.co.oportunidade;



import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;


@Getter
@Setter
public class Reference extends Domain {

    private final UUID id;
    private String entity;
    private String referenceNumber;
    private String currency;
    private List<Amount> amounts;
    private Double minAmount;
    private Double maxAmount;
    private Instant startDate;
    private Instant expirationDate;
    private Boolean isActive;
    private String createdBy;
    private String updatedBy;
    private Instant createdDate;
    private Instant updatedDate;

    // Constructors, getters, and setters
    public Reference() {
        id = UUID.randomUUID();
    }
}
