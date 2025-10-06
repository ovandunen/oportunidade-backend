package ao.co.oportunidade.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;


@Getter
@Setter
public class ReferenceDTO extends DTO {

        private UUID id;
        private String entity;
        private String referenceNumber;
        private String currency;
        private List<AmountDTO> amounts;
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
