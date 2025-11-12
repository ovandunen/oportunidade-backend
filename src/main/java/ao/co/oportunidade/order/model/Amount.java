package ao.co.oportunidade.order.model;

import solutions.envision.model.ValueObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;


@RequiredArgsConstructor
@Getter
@Setter
public class Amount implements ValueObject  {

    private final Double amount;
    private final String descriptionLine1;
    private final String descriptionLine2;
}
