package ao.co.oportunidade.entity;

import ao.co.oportunidade.Domain;
import ao.co.oportunidade.DomainEntity;

public interface EntityMapper<D extends Domain, E extends DomainEntity> {

    E mapDomainToEntity(D domain);
    D mapEntityToDomain(E entity);
}
