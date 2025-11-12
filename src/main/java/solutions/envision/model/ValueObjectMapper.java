package solutions.envision.model;


public interface ValueObjectMapper<V extends ValueObject, DV extends DomainValue> {

    DV mapToDomainValue(ValueObject value);
    V mapToValueObject(DV domainValue);
}
