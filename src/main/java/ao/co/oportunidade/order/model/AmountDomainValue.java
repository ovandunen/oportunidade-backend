package ao.co.oportunidade.order.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import solutions.envision.model.DomainValue;

@Embeddable
@Getter
@Setter
@RequiredArgsConstructor
public class AmountDomainValue implements DomainValue {

    private Double amount;

    @Column(name = "description_line_1")
    private String descriptionLine1;

    @Column(name = "description_line_2")
    private String descriptionLine2;
}
