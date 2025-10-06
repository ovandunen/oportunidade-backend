package ao.co.oportunidade.valueobject;


import ao.co.oportunidade.Amount;
import org.mapstruct.Mapper;

@Mapper
public interface AmountValueObjectMapper extends ValueObjectMapper<Amount,AmountDomainValue>{
}
