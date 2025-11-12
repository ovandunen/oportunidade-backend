package ao.co.oportunidade.order.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AmountDTO {

    private Double amount;
    private String descriptionLine1;
    private String descriptionLine2;

}
