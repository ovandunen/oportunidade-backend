package ao.co.oportunidade.order.model;

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

    private  Double amount;
    private  String descriptionLine1;
    private  String descriptionLine2;
}
