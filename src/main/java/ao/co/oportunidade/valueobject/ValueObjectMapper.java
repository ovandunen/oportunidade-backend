package ao.co.oportunidade.valueobject;

import org.mapstruct.Mapper;

@Mapper
public interface ValueObjectMapper<VO extends ValueObject, DV extends DomainValue> {

}
