package ao.co.oportunidade.order.model;


import org.mapstruct.Mapper;
import solutions.envision.model.ValueObjectMapper;

@Mapper
public interface AmountValueObjectMapper extends ValueObjectMapper<Amount,AmountDomainValue> {
}
