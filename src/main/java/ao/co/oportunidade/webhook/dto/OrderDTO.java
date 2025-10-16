package ao.co.oportunidade.webhook.dto;

import ao.co.oportunidade.dto.DTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for Order exposed in API layer.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO extends DTO {

    private UUID id;
    private String merchantTransactionId;
    private UUID referenceId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private Instant createdDate;
    private Instant updatedDate;
}
