package ao.co.oportunidade.valueobject;

import org.mapstruct.Mapper;

@Mapper
public interface ValueObjectMapper<VO extends ValueObject, DV extends DomainValue> {

public interface ValueObjectMapper<V extends ValueObject, DV extends DomainValue> {

    DV mapToDomainValue(ValueObject value);
    V mapToValueObject(DV domainValue);
}
